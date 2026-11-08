package com.mylog.journal.application.result;

/**
 * Outcome of one companion turn. {@code reply} is null unless {@code status} is {@code REPLY} or
 * {@code BLOCKED} (which carries a fixed neutral sentence only, no questions or actions).
 */
public record CompanionTurnResult(String status, CompanionReply reply) {
    public static CompanionTurnResult of(String status) {
        return new CompanionTurnResult(status, null);
    }

    public static CompanionTurnResult of(String status, CompanionReply reply) {
        return new CompanionTurnResult(status, reply);
    }
}
