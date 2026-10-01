package com.mylog.platform.crypto;

import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
final class SensitiveDataKeys {
    private final Map<String, SecretKey> encryptionKeys;
    private final SecretKey lookupKey;
    private final String version;

    SensitiveDataKeys(Environment environment) {
        boolean development = environment.matchesProfiles("local", "test");
        String encoded = environment.getProperty("MYLOG_IDENTITY_MASTER_KEY");
        if (encoded == null || encoded.isBlank()) {
            if (!development) {
                throw new IllegalStateException("Identity KMS/secret-manager key is required outside local/test");
            }
            // Fixed development key: never use this fallback outside local/test.
            encoded = Base64.getEncoder().encodeToString(
                    MessageDigestHolder.devKey());
        }
        version = environment.getProperty("MYLOG_IDENTITY_KEY_VERSION", development ? "local-v1" : "");
        if (version.isBlank()) throw new IllegalStateException("Identity key version is required");
        byte[] raw = decodeKey(encoded);
        var ring = new HashMap<String, SecretKey>();
        ring.put(version, new SecretKeySpec(raw, "AES"));
        String previous = environment.getProperty("MYLOG_IDENTITY_PREVIOUS_KEYS", "");
        if (!previous.isBlank()) {
            for (String entry : previous.split(",")) {
                String[] parts = entry.split(":", 2);
                if (parts.length != 2 || parts[0].isBlank() || ring.containsKey(parts[0]))
                    throw new IllegalStateException("Invalid previous identity key configuration");
                ring.put(parts[0], new SecretKeySpec(decodeKey(parts[1]), "AES"));
            }
        }
        encryptionKeys = Map.copyOf(ring);
        String lookupEncoded = environment.getProperty("MYLOG_IDENTITY_LOOKUP_KEY");
        if (lookupEncoded == null || lookupEncoded.isBlank()) {
            if (!development) throw new IllegalStateException("Stable identity lookup key is required outside local/test");
            lookupEncoded = Base64.getEncoder().encodeToString(MessageDigestHolder.devKey());
        }
        lookupKey = new SecretKeySpec(derive(decodeKey(lookupEncoded), "mylog-identity-lookup"), "HmacSHA256");
    }

    SecretKey encryptionKey(String keyVersion) {
        SecretKey key = encryptionKeys.get(keyVersion);
        if (key == null) throw new IllegalStateException("Unsupported identity key version");
        return key;
    }
    SecretKey lookupKey() { return lookupKey; }
    String version() { return version; }

    private static byte[] decodeKey(String encoded) {
        byte[] raw = Base64.getDecoder().decode(encoded);
        if (raw.length != 32) throw new IllegalStateException("Identity key must be 32 bytes");
        return raw;
    }

    private static byte[] derive(byte[] raw, String label) {
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(raw, "HmacSHA256"));
            return mac.doFinal(label.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { throw new IllegalStateException("Cannot derive identity key", e); }
    }

    private static final class MessageDigestHolder {
        static byte[] devKey() {
            try { return MessageDigest.getInstance("SHA-256").digest("mylog-local-test-only-key".getBytes(StandardCharsets.UTF_8)); }
            catch (Exception e) { throw new IllegalStateException(e); }
        }
    }
}
