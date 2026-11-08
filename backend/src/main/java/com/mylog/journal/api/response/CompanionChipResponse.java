package com.mylog.journal.api.response;

import com.mylog.journal.application.result.CompanionChip;

public record CompanionChipResponse(String label, String intent) {
    public static CompanionChipResponse from(CompanionChip chip) {
        return new CompanionChipResponse(chip.label(), chip.intent());
    }
}
