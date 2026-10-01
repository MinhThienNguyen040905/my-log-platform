package com.mylog.analysis.application;

import java.math.BigDecimal;
import java.util.List;

public interface JournalAnalyzer {
    Result analyze(String title, String plainText);

    record ScoredCode(String code, BigDecimal score) {}
    record Result(String sentiment, BigDecimal sentimentScore, List<ScoredCode> emotions,
                  List<ScoredCode> topics, String reflection, String provider, String model,
                  String modelVersion, String promptVersion, int inputTokens, int outputTokens,
                  BigDecimal estimatedCostUsd) {}
}
