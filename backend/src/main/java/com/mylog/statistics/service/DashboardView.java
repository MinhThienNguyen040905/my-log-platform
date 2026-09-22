package com.mylog.statistics.service;

import java.time.LocalDate;

public record DashboardView(
        LocalDate from,
        LocalDate to,
        String timezone,
        String calculationVersion,
        MoodStatisticsView mood,
        EmotionStatisticsView emotions,
        TopicStatisticsView topics) {}
