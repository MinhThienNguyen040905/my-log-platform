package com.mylog.insight.api.response;

import com.mylog.insight.application.ThinkingPatternService;

import java.util.UUID;

/** {@code created} is false when the plan already existed (the call is safe to repeat). */
public record PatternPlanResponse(UUID goalId, boolean created) {
    public static PatternPlanResponse from(ThinkingPatternService.PlanStarted plan) {
        return new PatternPlanResponse(plan.goalId(), plan.created());
    }
}
