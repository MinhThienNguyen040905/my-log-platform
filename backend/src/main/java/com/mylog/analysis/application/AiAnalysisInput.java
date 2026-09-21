package com.mylog.analysis.application;

import java.util.UUID;

public record AiAnalysisInput(
        UUID journalId,
        long journalVersion,
        String content,
        int moodScore,
        Integer stressScore,
        Integer energyScore,
        int maxReflections) {}
