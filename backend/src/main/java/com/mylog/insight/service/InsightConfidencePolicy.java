package com.mylog.insight.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class InsightConfidencePolicy {

    public String classify(int distinctDays, BigDecimal matchingRatio) {
        if (distinctDays >= 8 && matchingRatio.compareTo(new BigDecimal("0.75")) >= 0) return "STRONG";
        if (distinctDays >= 5 && matchingRatio.compareTo(new BigDecimal("0.60")) >= 0) return "MODERATE";
        if (distinctDays >= 3 && matchingRatio.compareTo(new BigDecimal("0.50")) >= 0) return "WEAK";
        return null;
    }
}
