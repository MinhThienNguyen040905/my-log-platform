package com.mylog.journal.api.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CompanionContextRequest(@Size(max = 32) String moodCode,
                                      @DecimalMin("0") @DecimalMax("10") BigDecimal stressScore,
                                      @DecimalMin("0") @DecimalMax("10") BigDecimal energyScore,
                                      @Min(0) @Max(1440) Integer sleepMinutes,
                                      @Min(0) @Max(23) Integer localHour) {}
