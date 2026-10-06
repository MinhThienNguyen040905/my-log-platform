package com.mylog.checkin.application.command;

import java.math.BigDecimal;
import java.util.List;

public record PutCheckinCommand(String timezone, String moodCode, BigDecimal moodScore,
                                BigDecimal stressScore, BigDecimal energyScore, Integer sleepMinutes,
                                String note, List<ActivityCommand> activities) {
    public record ActivityCommand(String code, Integer durationMinutes, String intensity) {}
}
