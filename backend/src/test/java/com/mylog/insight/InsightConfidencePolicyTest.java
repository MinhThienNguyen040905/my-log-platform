package com.mylog.insight;

import static org.assertj.core.api.Assertions.assertThat;

import com.mylog.insight.service.InsightConfidencePolicy;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InsightConfidencePolicyTest {

    private final InsightConfidencePolicy policy = new InsightConfidencePolicy();

    @Test
    void requiresBothMinimumDistinctDaysAndMatchingRatio() {
        assertThat(policy.classify(2, new BigDecimal("1.00"))).isNull();
        assertThat(policy.classify(3, new BigDecimal("0.49"))).isNull();
        assertThat(policy.classify(3, new BigDecimal("0.50"))).isEqualTo("WEAK");
        assertThat(policy.classify(5, new BigDecimal("0.60"))).isEqualTo("MODERATE");
        assertThat(policy.classify(8, new BigDecimal("0.75"))).isEqualTo("STRONG");
    }
}
