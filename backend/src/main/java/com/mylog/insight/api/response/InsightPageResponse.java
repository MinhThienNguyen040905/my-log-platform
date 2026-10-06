package com.mylog.insight.api.response;

import com.mylog.insight.application.query.InsightPage;

import java.util.List;

public record InsightPageResponse(List<InsightResponse> items, String nextCursor) {
    public static InsightPageResponse from(InsightPage page) {
        return new InsightPageResponse(page.items().stream().map(InsightResponse::from).toList(),
                page.nextCursor());
    }
}
