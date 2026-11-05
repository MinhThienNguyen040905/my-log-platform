package com.mylog.identity.application;

import com.mylog.identity.api.request.GoogleAuthRequest;
import com.mylog.identity.application.result.AuthResult;
import com.mylog.identity.infrastructure.GoogleOAuthClient.GoogleTokenPayload;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import com.mylog.user.application.UserProfileUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class GoogleAuthProvisioningService {
    private final IdentityStore store;
    private final SensitiveDataCipher cipher;
    private final IdGenerator ids;
    private final UserProfileUseCase profiles;
    private final AccessTokens accessTokens;

    public GoogleAuthProvisioningService(IdentityStore store, SensitiveDataCipher cipher,
                                         IdGenerator ids, UserProfileUseCase profiles,
                                         AccessTokens accessTokens) {
        this.store = store;
        this.cipher = cipher;
        this.ids = ids;
        this.profiles = profiles;
        this.accessTokens = accessTokens;
    }

    @Transactional
    public AuthResult completeGoogleAuthTransaction(GoogleTokenPayload payload,
                                                    GoogleAuthRequest request,
                                                    String randomToken,
                                                    Instant now) {
        String sub = payload.sub();
        String normalizedEmail = payload.email().toLowerCase(Locale.ROOT).trim();
        byte[] emailHash = cipher.lookupHash(normalizedEmail);

        UUID userId;
        UUID linkedUserId = store.findUserIdByOAuth("GOOGLE", sub).orElse(null);

        if (linkedUserId != null) {
            // Trường hợp 1: Đã từng liên kết Google sub trước đó
            IdentityStore.Account account = store.accountById(linkedUserId)
                    .orElseThrow(IdentityService.InvalidCredentialsException::new);

            // KIỂM TRA TRẠNG THÁI TRƯỚC KHI THAO TÁC GHI
            if (!"ACTIVE".equals(account.status()) || (account.lockedUntil() != null && account.lockedUntil().isAfter(now))) {
                throw new IdentityService.InvalidCredentialsException();
            }
            userId = account.id();
            store.setLoginSuccess(userId, now);
            store.audit(userId, "LOGIN_GOOGLE", userId, now);
        } else {
            // Trường hợp 2: Chưa liên kết Google sub -> Tra cứu theo email
            IdentityStore.Account existing = store.accountByEmailHashForUpdate(emailHash).orElse(null);

            if (existing == null) {
                // Nhánh 2A: Tài khoản mới hoàn toàn (JIT Provisioning)
                userId = ids.next();
                var encrypted = cipher.encrypt("users.email", userId, userId, normalizedEmail);
                IdentityStore.Account newAccount = new IdentityStore.Account(
                        userId, emailHash, encrypted, null, "ACTIVE", 0, null, 0, now
                );
                store.createOAuthAccount(newAccount, "GOOGLE", sub, now);
                store.assignUserRole(userId, now);
                profiles.createDefault(userId, request.timezone(), request.locale(), "v1", "v1", now);
                store.audit(userId, "REGISTER_GOOGLE", userId, now);
            } else {
                // Nhánh 2B: Email đã tồn tại trong hệ thống (Account Linking)
                // KIỂM TRA TRẠNG THÁI TRƯỚC KHI THAO TÁC GHI
                if (!"ACTIVE".equals(existing.status()) || (existing.lockedUntil() != null && existing.lockedUntil().isAfter(now))) {
                    throw new IdentityService.InvalidCredentialsException();
                }
                userId = existing.id();

                if (existing.emailVerifiedAt() == null) {
                    // PHÒNG THỦ PRE-ACCOUNT-TAKEOVER:
                    // 1. Chuyển auth_provider sang GOOGLE và xóa password_hash (thỏa mãn CHECK constraint)
                    store.reclaimUnverifiedAccountForOAuth(userId, "GOOGLE", now);
                    // 2. Thu hồi toàn bộ sessions cũ do kẻ tấn công tạo
                    store.revokeAllSessions(userId, "PRE_TAKEOVER_DEFENSE", now);
                    // 3. Xóa pending_registrations nếu có
                    store.pendingByEmailHashForUpdate(emailHash).ifPresent(p -> store.deletePendingRegistration(p.id()));
                    store.audit(userId, "RECLAIM_UNVERIFIED_ACCOUNT", userId, now);
                }

                store.linkOAuthIdentity(userId, "GOOGLE", sub, now);
                store.setLoginSuccess(userId, now);
                store.audit(userId, "LINK_GOOGLE", userId, now);
            }
        }

        // Cấp phát phiên đăng nhập
        UUID sessionId = ids.next();
        store.createSession(new IdentityStore.Session(
                sessionId, userId, ids.next(), cipher.tokenHash(randomToken),
                now.plus(30, ChronoUnit.DAYS), null, now, now, request.deviceName()
        ));

        return new AuthResult(userId, accessTokens.issue(userId, sessionId), randomToken, 600);
    }
}
