package com.mylog.journal.api.response;

import com.mylog.journal.application.query.JournalPage;

import java.util.List;

public record JournalPageResponse(List<JournalSummaryResponse> items, String nextCursor) {
    public static JournalPageResponse from(JournalPage page) {
        return new JournalPageResponse(page.entries().stream().map(JournalSummaryResponse::from).toList(),
                page.nextCursor());
    }
}
