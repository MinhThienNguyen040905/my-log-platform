package com.mylog.identity.api.response;

import com.mylog.identity.application.DevAccountService;

import java.util.UUID;

public record DevAccountResponse(UUID id, String status, String email) {
    public static DevAccountResponse from(DevAccountService.Account account) {
        return new DevAccountResponse(account.id(), account.status(), account.email());
    }
}
