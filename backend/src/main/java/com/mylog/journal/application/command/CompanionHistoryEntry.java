package com.mylog.journal.application.command;

/** One prior turn of the ephemeral companion conversation. Never persisted. */
public record CompanionHistoryEntry(String role, String text) {
    public boolean isUser() {
        return "user".equals(role);
    }
}
