package com.mylog.insight.application.query;

import java.math.BigDecimal;

public record TopCode(String code, long count, BigDecimal averageScore) {}
