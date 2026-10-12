package com.mylog.identity.application;

import com.mylog.platform.crypto.SensitiveDataCipher;

import java.util.List;
import java.util.UUID;

public interface DevAccountStore {
    record Account(UUID id, String status, SensitiveDataCipher.Encrypted email) {}
    List<Account> list(UUID beforeId, int limit);
}
