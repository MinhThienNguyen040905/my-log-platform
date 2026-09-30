package com.mylog.journal.application;

import com.mylog.platform.crypto.SensitiveDataCipher;

import java.util.UUID;

public interface JournalContentCipher {
    SensitiveDataCipher.Encrypted encrypt(UUID userId, UUID entryId, String payload);
    String decrypt(UUID userId, UUID entryId, SensitiveDataCipher.Encrypted encrypted);
    byte[] lookupHash(String normalizedTag);
}
