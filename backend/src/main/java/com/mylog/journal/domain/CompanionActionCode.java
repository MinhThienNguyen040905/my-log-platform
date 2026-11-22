package com.mylog.journal.domain;

/**
 * Fixed catalogue of immediate calming actions the companion may offer. The companion (and any
 * future LLM-backed responder) can only pick from this enum; no adapter may invent a new action
 * code. Each code must have a widget already built and approved on the frontend before it is added.
 * Only codes marked {@link #aiSelectable()} may be chosen by the LLM; the rest come from templates.
 */
public enum CompanionActionCode {
    BREATHING_2MIN("Thở chậm 2 phút", true),
    AMBIENT("Nghe âm thanh dịu", true),
    VENT_PROMPT("Viết xả ra trang", false),
    TITLE("Gợi ý tựa đề", false);

    /** Most actions an LLM-chosen turn may carry. */
    public static final int MAX_AI_ACTIONS = 2;

    private final String label;
    private final boolean aiSelectable;

    CompanionActionCode(String label, boolean aiSelectable) {
        this.label = label;
        this.aiSelectable = aiSelectable;
    }

    public String label() {
        return label;
    }

    /** Whether the generative companion may pick this action on its own (only the calming tools). */
    public boolean aiSelectable() {
        return aiSelectable;
    }
}
