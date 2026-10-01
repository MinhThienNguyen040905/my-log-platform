package com.mylog.safety.application;

import java.util.UUID;

public interface SafetyAnalysisPermission {
    boolean permits(UUID userId, UUID entryId);
    String policyVersion(UUID userId, UUID entryId);
}
