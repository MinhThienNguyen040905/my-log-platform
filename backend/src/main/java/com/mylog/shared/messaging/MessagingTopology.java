package com.mylog.shared.messaging;

public final class MessagingTopology {

    public static final String EVENTS_EXCHANGE = "mylog.events";
    public static final String DEAD_LETTER_EXCHANGE = "mylog.dead.exchange";
    public static final String JOURNAL_EVENTS_QUEUE = "mylog.journal.events";
    public static final String ANALYSIS_QUEUE = "mylog.analysis.requested";
    public static final String REFLECTION_QUEUE = "mylog.reflection.requested";
    public static final String STATISTICS_QUEUE = "mylog.statistics.requested";
    public static final String INSIGHT_QUEUE = "mylog.insight.requested";
    public static final String DEAD_LETTER_QUEUE = "mylog.dead";

    public static final String JOURNAL_CREATED = "journal.created";
    public static final String JOURNAL_UPDATED = "journal.updated";
    public static final String JOURNAL_DELETED = "journal.deleted";
    public static final String JOURNAL_ANALYSIS_REQUESTED = "journal.analysis.requested";
    public static final String JOURNAL_ANALYSIS_COMPLETED = "journal.analysis.completed";
    public static final String JOURNAL_ANALYSIS_FAILED = "journal.analysis.failed";
    public static final String JOURNAL_CORRECTED = "journal.corrected";
    public static final String STATISTICS_UPDATED = "statistics.updated";

    private MessagingTopology() {}
}
