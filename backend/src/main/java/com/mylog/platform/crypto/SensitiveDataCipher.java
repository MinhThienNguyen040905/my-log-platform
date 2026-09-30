package com.mylog.platform.crypto;

import java.util.UUID;

public interface SensitiveDataCipher {
    Encrypted encrypt(String table, UUID owner, UUID row, String value);
    String decrypt(String table, UUID owner, UUID row, Encrypted encrypted);
    byte[] lookupHash(String normalizedValue);
    byte[] tokenHash(String token);

    record Encrypted(byte[] ciphertext, byte[] iv, byte[] wrappedKey, String keyVersion) {}
}
