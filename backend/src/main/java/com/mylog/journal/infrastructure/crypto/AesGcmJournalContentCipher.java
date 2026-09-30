package com.mylog.journal.infrastructure.crypto;

import com.mylog.journal.application.JournalContentCipher;
import com.mylog.platform.crypto.SensitiveDataCipher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
final class AesGcmJournalContentCipher implements JournalContentCipher {
    private final SensitiveDataCipher cipher;

    AesGcmJournalContentCipher(SensitiveDataCipher cipher) { this.cipher = cipher; }

    @Override public SensitiveDataCipher.Encrypted encrypt(UUID userId, UUID entryId, String payload) {
        return cipher.encrypt("journal_entries.payload", userId, entryId, payload);
    }

    @Override public String decrypt(UUID userId, UUID entryId, SensitiveDataCipher.Encrypted encrypted) {
        return cipher.decrypt("journal_entries.payload", userId, entryId, encrypted);
    }

    @Override public byte[] lookupHash(String normalizedTag) {
        return cipher.lookupHash("journal-tag:" + normalizedTag);
    }
}
