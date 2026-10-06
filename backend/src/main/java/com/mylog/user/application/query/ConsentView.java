package com.mylog.user.application.query;

import java.time.Instant;

public record ConsentView(String type, String documentVersion, boolean granted, Instant decidedAt) {}
