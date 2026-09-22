package com.mylog.identity.service;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class EmailNormalizer {

    public String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
