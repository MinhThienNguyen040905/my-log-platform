package com.mylog.reporting.application.query;

import java.util.List;

public record ReportPage(List<ReportView> items, String nextCursor) {}
