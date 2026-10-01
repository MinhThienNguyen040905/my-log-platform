package com.mylog.insight.application.query;

import java.util.List;

public record InsightPage(List<InsightView> items, String nextCursor) {}
