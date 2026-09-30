package com.mylog.safety.application.query;

import java.util.UUID;

public record SafetyResourceView(UUID id, String name, String resourceType, String contactValue,
                                 String description, String sourceUrl, int version) {}
