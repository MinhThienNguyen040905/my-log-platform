package com.mylog.journal.infrastructure;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mylog.journal.application.JournalAssetDeletion;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class CloudinaryJournalAssetDeletion implements JournalAssetDeletion {
    private final EntityManager em;
    private final ObjectProvider<Cloudinary> cloudinary;
    private final TransactionTemplate transactions;
    CloudinaryJournalAssetDeletion(EntityManager em,ObjectProvider<Cloudinary> cloudinary,TransactionTemplate transactions) {
        this.em=em;this.cloudinary=cloudinary;this.transactions=transactions;
    }
    record Asset(UUID id,String publicId,String assetType,String deliveryType) {}
    @Override public void deleteAll(UUID userId) {
        List<Asset> assets=transactions.execute(s -> {
            List<?> values=em.createNativeQuery("""
                SELECT id, public_id, asset_type, delivery_type FROM journal_assets
                WHERE user_id=:user AND status<>'DELETED'
                """).setParameter("user",userId).getResultList();
            return values.stream().map(value -> (Object[]) value)
                    .map(row -> new Asset((UUID)row[0],(String)row[1],(String)row[2],(String)row[3])).toList();
        });
        if (assets==null || assets.isEmpty()) return;
        Cloudinary provider=cloudinary.getIfAvailable();
        if (provider==null) throw new IllegalStateException("Cloudinary cleanup unavailable");
        for (Asset asset:assets) {
            try {
                var response=provider.uploader().destroy(asset.publicId(),ObjectUtils.asMap(
                        "resource_type","IMAGE".equals(asset.assetType())?"image":"raw",
                        "type",asset.deliveryType().toLowerCase(java.util.Locale.ROOT),"invalidate",true));
                if (!"ok".equals(response.get("result")) && !"not found".equals(response.get("result")))
                    throw new IllegalStateException("Cloudinary deletion incomplete");
                transactions.executeWithoutResult(s -> em.createNativeQuery("""
                        UPDATE journal_assets SET status='DELETED', deleted_at=CURRENT_TIMESTAMP
                        WHERE id=:id AND user_id=:user
                        """).setParameter("id",asset.id()).setParameter("user",userId).executeUpdate());
            } catch (Exception e) { throw new IllegalStateException("Cloudinary deletion failed",e); }
        }
    }
}
