package com.mylog.journal.domain;

/**
 * Fixed catalogue of immediate calming actions the companion may offer. The companion (and any
 * future LLM-backed responder) can only pick from this enum; no adapter may invent a new action
 * code. Each code must have a widget already built and approved on the frontend before it is added.
 */
public enum CompanionActionCode {
    BREATHING_2MIN("Thở chậm 2 phút"),
    AMBIENT("Nghe âm thanh dịu"),
    VENT_PROMPT("Viết xả ra trang"),
    TITLE("Gợi ý tựa đề");

    private final String label;

    CompanionActionCode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
