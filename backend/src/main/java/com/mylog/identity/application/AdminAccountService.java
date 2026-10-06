package com.mylog.identity.application;

import com.mylog.platform.web.ConflictException;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.platform.web.ResourceNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class AdminAccountService {
    private final AdminAccountStore store;
    private final Clock clock;
    public AdminAccountService(AdminAccountStore store, Clock clock) { this.store=store; this.clock=clock; }
    @Transactional(readOnly = true)
    public List<AdminAccountStore.Metadata> list(UUID before, int limit) {
        if (limit<1 || limit>100) throw new InvalidRequestException();
        return store.list(before,limit);
    }
    @Transactional(readOnly = true)
    public AdminAccountStore.Metadata get(UUID id) {
        return store.get(id).orElseThrow(() -> new ResourceNotFoundException("User không tồn tại."));
    }
    @Transactional
    public AdminAccountStore.Metadata suspend(UUID actor, UUID target, String reasonCode) {
        reason(reasonCode);
        if (actor.equals(target)) throw new ConflictException("Không thể tự đình chỉ tài khoản.");
        if (!store.changeStatus(target,"ACTIVE","SUSPENDED",clock.instant()))
            throw new ConflictException("Tài khoản không ở trạng thái active.");
        store.revokeSessions(target,clock.instant());
        store.audit(actor,"USER_SUSPENDED",target,reasonCode,clock.instant());
        return get(target);
    }
    @Transactional
    public AdminAccountStore.Metadata restore(UUID actor, UUID target, String reasonCode) {
        reason(reasonCode);
        if (!store.changeStatus(target,"SUSPENDED","ACTIVE",clock.instant()))
            throw new ConflictException("Tài khoản không ở trạng thái suspended.");
        store.audit(actor,"USER_RESTORED",target,reasonCode,clock.instant());
        return get(target);
    }
    private static void reason(String code) {
        if (code==null || !code.matches("[A-Z0-9_]{3,60}")) throw new InvalidRequestException();
    }
}
