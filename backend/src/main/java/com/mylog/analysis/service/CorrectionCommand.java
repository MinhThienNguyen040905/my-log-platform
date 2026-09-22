package com.mylog.analysis.service;

import java.math.BigDecimal;
import java.util.List;

public record CorrectionCommand(
        List<EmotionCorrection> emotions,
        List<TopicCorrection> topics) {

    public record EmotionCorrection(String type, Operation operation, BigDecimal score) {}
    public record TopicCorrection(String name, Operation operation, BigDecimal confidence) {}
    public enum Operation { ADD, UPDATE, REMOVE }
}
