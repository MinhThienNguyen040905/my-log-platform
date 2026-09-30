package com.mylog.journal.application.query;

import java.util.List;

public record JournalPage(List<JournalSummaryView> entries, String nextCursor) {}
