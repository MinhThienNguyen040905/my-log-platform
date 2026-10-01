package com.mylog.analysis.application.query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AnalysisView(UUID entryId, int contentVersion, String status, UUID analysisId,
                           String sentiment, BigDecimal sentimentScore, List<String> emotions,
                           List<String> topics, String reflection) {}
