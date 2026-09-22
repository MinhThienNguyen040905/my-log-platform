package com.mylog.identity.service;

public record AuthenticationResult(IssuedTokens tokens, UserProfile user) {}
