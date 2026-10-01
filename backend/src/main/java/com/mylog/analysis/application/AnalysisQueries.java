package com.mylog.analysis.application;

import com.mylog.analysis.application.query.AnalysisView;

import java.util.UUID;

public interface AnalysisQueries {
    AnalysisView get(UUID userId, UUID entryId);
    AnalysisView exportRetained(UUID userId, UUID entryId);
}
