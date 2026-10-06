package com.mylog.insight.api.response;

import com.mylog.insight.application.query.TopCode;

import java.math.BigDecimal;

public record TopCodeResponse(String code, long count, BigDecimal averageScore) {
    public static TopCodeResponse from(TopCode value) {
        return new TopCodeResponse(value.code(), value.count(), value.averageScore());
    }
}
