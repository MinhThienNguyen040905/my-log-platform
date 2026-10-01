package com.mylog.reporting.api.response;

import com.mylog.reporting.application.query.ReportPage;

import java.util.List;

public record ReportPageResponse(List<ReportResponse> items, String nextCursor) {
    public static ReportPageResponse from(ReportPage page) {
        return new ReportPageResponse(page.items().stream().map(ReportResponse::from).toList(),
                page.nextCursor());
    }
}
