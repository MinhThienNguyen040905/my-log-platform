package com.mylog.analysis.api.response;

import com.mylog.analysis.application.PatternExtractionService;

import java.time.LocalDate;

public record DevPatternExtractionResponse(String status, int read, int signals, int skipped, int failed,
                                           boolean more, LocalDate from, LocalDate to) {
    public static DevPatternExtractionResponse from(PatternExtractionService.Summary summary) {
        return new DevPatternExtractionResponse(summary.status(), summary.read(), summary.signals(), summary.skipped(),
                summary.failed(), summary.more(), summary.from(), summary.to());
    }
}
