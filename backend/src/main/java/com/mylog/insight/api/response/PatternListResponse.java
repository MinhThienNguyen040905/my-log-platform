package com.mylog.insight.api.response;

import com.mylog.insight.application.query.ThinkingPatternView;

import java.util.List;

public record PatternListResponse(List<PatternResponse> items) {
    public static PatternListResponse from(List<ThinkingPatternView> views) {
        return new PatternListResponse(views.stream().map(PatternResponse::from).toList());
    }
}
