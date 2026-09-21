package com.mylog.identity.application;

public record AuthenticationResult(IssuedTokens tokens, UserProfile user) {}
