package com.mylog.statistics.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EmotionStatisticsView(
        LocalDate from, LocalDate to, String timezone, String calculationVersion,
        List<Emotion> emotions) {

    public record Emotion(String type, int sampleSize, BigDecimal averageScore, BigDecimal share) {}
}
