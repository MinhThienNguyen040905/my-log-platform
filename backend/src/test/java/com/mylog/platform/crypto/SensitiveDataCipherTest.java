package com.mylog.platform.crypto;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveDataCipherTest {
    private static final String OLD_KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String NEW_KEY = Base64.getEncoder().encodeToString(new byte[]{
            1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32});
    private static final String LOOKUP = Base64.getEncoder().encodeToString(new byte[]{
            32,31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1});

    @Test void tamperingWrongOwnerAndKeyRotation() {
        var oldEnv = new MockEnvironment().withProperty("MYLOG_IDENTITY_MASTER_KEY", OLD_KEY)
                .withProperty("MYLOG_IDENTITY_LOOKUP_KEY", LOOKUP)
                .withProperty("MYLOG_IDENTITY_KEY_VERSION", "old-v1");
        oldEnv.setActiveProfiles("test");
        var newEnv = new MockEnvironment().withProperty("MYLOG_IDENTITY_MASTER_KEY", NEW_KEY)
                .withProperty("MYLOG_IDENTITY_LOOKUP_KEY", LOOKUP)
                .withProperty("MYLOG_IDENTITY_KEY_VERSION", "new-v2")
                .withProperty("MYLOG_IDENTITY_PREVIOUS_KEYS", "old-v1:" + OLD_KEY);
        newEnv.setActiveProfiles("test");
        var oldCipher = new AesGcmSensitiveDataCipher(new SensitiveDataKeys(oldEnv), new SecureRandom());
        var newCipher = new AesGcmSensitiveDataCipher(new SensitiveDataKeys(newEnv), new SecureRandom());
        UUID owner = UUID.randomUUID();
        UUID row = UUID.randomUUID();
        var first = oldCipher.encrypt("users.email", owner, row, "person@example.test");
        var second = oldCipher.encrypt("users.email", owner, row, "person@example.test");
        assertFalse(Arrays.equals(first.ciphertext(), second.ciphertext()));
        assertEquals("person@example.test", newCipher.decrypt("users.email", owner, row, first));
        assertArrayEquals(oldCipher.lookupHash("person@example.test"), newCipher.lookupHash("person@example.test"));
        assertThrows(IllegalStateException.class, () -> newCipher.decrypt("users.email", UUID.randomUUID(), row, first));
        byte[] changed = first.ciphertext().clone();
        changed[0] ^= 1;
        var tampered = new SensitiveDataCipher.Encrypted(changed, first.iv(), first.wrappedKey(), first.keyVersion());
        assertThrows(IllegalStateException.class, () -> newCipher.decrypt("users.email", owner, row, tampered));
    }
}
