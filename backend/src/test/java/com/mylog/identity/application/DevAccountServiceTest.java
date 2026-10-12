package com.mylog.identity.application;

import com.mylog.platform.crypto.SensitiveDataCipher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevAccountServiceTest {
    @Test void decryptsEachEmailWithItsOwnUserId() {
        DevAccountStore store = mock(DevAccountStore.class);
        SensitiveDataCipher cipher = mock(SensitiveDataCipher.class);
        UUID id = UUID.randomUUID();
        var encrypted = new SensitiveDataCipher.Encrypted(new byte[1], new byte[1], new byte[1], "test");
        when(store.list(null, 50)).thenReturn(List.of(new DevAccountStore.Account(id, "ACTIVE", encrypted)));
        when(cipher.decrypt("users.email", id, id, encrypted)).thenReturn("person@example.test");

        var result = new DevAccountService(store, cipher).list(null, 50);

        assertEquals(List.of(new DevAccountService.Account(id, "ACTIVE", "person@example.test")), result);
        verify(cipher).decrypt("users.email", id, id, encrypted);
    }
}
