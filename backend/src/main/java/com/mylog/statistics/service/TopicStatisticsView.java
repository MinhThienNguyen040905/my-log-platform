package com.mylog.statistics.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TopicStatisticsView(
        LocalDate from, LocalDate to, String timezone, String calculationVersion,
        int journalCount, List<Topic> topics) {

    public record Topic(String name, int journalCount, BigDecimal frequency) {}
}
