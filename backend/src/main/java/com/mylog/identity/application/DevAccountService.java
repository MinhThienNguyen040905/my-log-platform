package com.mylog.identity.application;

import com.mylog.platform.crypto.SensitiveDataCipher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Profile("local & !staging & !prod")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class DevAccountService {
    private final DevAccountStore store;
    private final SensitiveDataCipher cipher;

    public DevAccountService(DevAccountStore store, SensitiveDataCipher cipher) {
        this.store = store;
        this.cipher = cipher;
    }

    @Transactional(readOnly = true)
    public List<Account> list(UUID beforeId, int limit) {
        return store.list(beforeId, limit).stream()
                .map(account -> new Account(account.id(), account.status(),
                        cipher.decrypt("users.email", account.id(), account.id(), account.email())))
                .toList();
    }

    public record Account(UUID id, String status, String email) {}
}
