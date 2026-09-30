package com.mylog.platform.crypto;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
final class AesGcmSensitiveDataCipher implements SensitiveDataCipher {
    private final SensitiveDataKeys keys;
    private final SecureRandom random;

    AesGcmSensitiveDataCipher(SensitiveDataKeys keys, SecureRandom random) {
        this.keys = keys;
        this.random = random;
    }

    @Override
    public Encrypted encrypt(String table, UUID owner, UUID row, String value) {
        try {
            byte[] dek = new byte[32]; random.nextBytes(dek);
            byte[] iv = new byte[12]; random.nextBytes(iv);
            byte[] wrapIv = new byte[12]; random.nextBytes(wrapIv);
            Cipher payload = Cipher.getInstance("AES/GCM/NoPadding");
            payload.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(dek, "AES"), new GCMParameterSpec(128, iv));
            payload.updateAAD(aad(table, owner, row));
            byte[] ciphertext = payload.doFinal(value.getBytes(StandardCharsets.UTF_8));
            Cipher wrapper = Cipher.getInstance("AES/GCM/NoPadding");
            wrapper.init(Cipher.ENCRYPT_MODE, keys.encryptionKey(keys.version()), new GCMParameterSpec(128, wrapIv));
            wrapper.updateAAD(aad(table + ":key", owner, row));
            byte[] wrapped = wrapper.doFinal(dek);
            Arrays.fill(dek, (byte) 0);
            return new Encrypted(ciphertext, iv, ByteBuffer.allocate(wrapIv.length + wrapped.length).put(wrapIv).put(wrapped).array(), keys.version());
        } catch (Exception e) { throw new IllegalStateException("Identity encryption failed"); }
    }

    @Override
    public String decrypt(String table, UUID owner, UUID row, Encrypted encrypted) {
        try {
            byte[] wrapIv = Arrays.copyOfRange(encrypted.wrappedKey(), 0, 12);
            byte[] wrapped = Arrays.copyOfRange(encrypted.wrappedKey(), 12, encrypted.wrappedKey().length);
            Cipher wrapper = Cipher.getInstance("AES/GCM/NoPadding");
            wrapper.init(Cipher.DECRYPT_MODE, keys.encryptionKey(encrypted.keyVersion()), new GCMParameterSpec(128, wrapIv));
            wrapper.updateAAD(aad(table + ":key", owner, row));
            byte[] dek = wrapper.doFinal(wrapped);
            Cipher payload = Cipher.getInstance("AES/GCM/NoPadding");
            payload.init(Cipher.DECRYPT_MODE, new SecretKeySpec(dek, "AES"), new GCMParameterSpec(128, encrypted.iv()));
            payload.updateAAD(aad(table, owner, row));
            byte[] plain = payload.doFinal(encrypted.ciphertext());
            Arrays.fill(dek, (byte) 0);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("Identity decryption failed"); }
    }

    @Override
    public byte[] lookupHash(String normalizedValue) { return hmac("lookup:" + normalizedValue); }
    @Override
    public byte[] tokenHash(String token) { return hmac("token:" + token); }

    private byte[] hmac(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256"); mac.init(keys.lookupKey());
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { throw new IllegalStateException("Identity HMAC failed"); }
    }

    private static byte[] aad(String table, UUID owner, UUID row) {
        return (table + ":" + owner + ":" + row + ":v1").getBytes(StandardCharsets.UTF_8);
    }
}
