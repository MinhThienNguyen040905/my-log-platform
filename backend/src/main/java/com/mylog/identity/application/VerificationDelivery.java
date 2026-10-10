package com.mylog.identity.application;

public interface VerificationDelivery {
    void send(String email, String token, String code);
}
