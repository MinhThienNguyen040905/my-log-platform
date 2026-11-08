package com.mylog.journal.api.response;

import com.mylog.journal.domain.CompanionActionCode;

public record CompanionActionResponse(String code, String label) {
    public static CompanionActionResponse from(CompanionActionCode code) {
        return new CompanionActionResponse(code.name(), code.label());
    }
}
