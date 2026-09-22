package com.mylog.statistics.dto;

import com.mylog.statistics.service.EmotionStatisticsView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EmotionStatisticsResponse(
        LocalDate from, LocalDate to, String timezone, String calculationVersion,
        List<Emotion> emotions) {

    public static EmotionStatisticsResponse from(EmotionStatisticsView view) {
        return new EmotionStatisticsResponse(
                view.from(), view.to(), view.timezone(), view.calculationVersion(),
                view.emotions().stream().map(item -> new Emotion(
                        item.type(), item.sampleSize(), item.averageScore(), item.share())).toList());
    }

    public record Emotion(String type, int sampleSize, BigDecimal averageScore, BigDecimal share) {}
}
