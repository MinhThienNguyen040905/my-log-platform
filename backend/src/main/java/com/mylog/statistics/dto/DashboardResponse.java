package com.mylog.statistics.dto;

import com.mylog.statistics.service.DashboardView;
import java.time.LocalDate;

public record DashboardResponse(
        LocalDate from, LocalDate to, String timezone, String calculationVersion,
        MoodStatisticsResponse mood,
        EmotionStatisticsResponse emotions,
        TopicStatisticsResponse topics) {

    public static DashboardResponse from(DashboardView view) {
        return new DashboardResponse(
                view.from(), view.to(), view.timezone(), view.calculationVersion(),
                MoodStatisticsResponse.from(view.mood()),
                EmotionStatisticsResponse.from(view.emotions()),
                TopicStatisticsResponse.from(view.topics()));
    }
}
