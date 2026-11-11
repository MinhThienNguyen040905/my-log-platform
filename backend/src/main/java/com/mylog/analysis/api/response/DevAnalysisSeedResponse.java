package com.mylog.analysis.api.response;

import com.mylog.analysis.application.DevAnalysisSeedService;

import java.time.LocalDate;

public record DevAnalysisSeedResponse(int analyzed, int skipped, LocalDate from, LocalDate to) {
    public static DevAnalysisSeedResponse from(DevAnalysisSeedService.Result result) {
        return new DevAnalysisSeedResponse(result.analyzed(), result.skipped(), result.from(), result.to());
    }
}
