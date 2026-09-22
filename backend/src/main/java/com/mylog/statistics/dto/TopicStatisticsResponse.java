package com.mylog.statistics.dto;

import com.mylog.statistics.service.TopicStatisticsView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TopicStatisticsResponse(
        LocalDate from, LocalDate to, String timezone, String calculationVersion,
        int journalCount, List<Topic> topics) {

    public static TopicStatisticsResponse from(TopicStatisticsView view) {
        return new TopicStatisticsResponse(
                view.from(), view.to(), view.timezone(), view.calculationVersion(), view.journalCount(),
                view.topics().stream().map(item -> new Topic(
                        item.name(), item.journalCount(), item.frequency())).toList());
    }

    public record Topic(String name, int journalCount, BigDecimal frequency) {}
}
