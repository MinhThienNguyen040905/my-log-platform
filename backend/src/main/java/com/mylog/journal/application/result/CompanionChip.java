package com.mylog.journal.application.result;

/** A quick-reply chip; the label the user sees and a client-only intent (never re-sent as free text). */
public record CompanionChip(String label, String intent) {}
