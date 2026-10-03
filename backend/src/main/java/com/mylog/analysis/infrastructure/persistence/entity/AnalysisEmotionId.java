package com.mylog.analysis.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.UUID;
import java.util.Objects;

public class AnalysisEmotionId implements Serializable {
    public UUID analysisId;
    public String emotionCode;

    public AnalysisEmotionId() {}

    public AnalysisEmotionId(UUID analysisId, String emotionCode) {
        this.analysisId = analysisId;
        this.emotionCode = emotionCode;
    }

    @Override public boolean equals(Object other) {
        return other instanceof AnalysisEmotionId id
                && Objects.equals(analysisId, id.analysisId)
                && Objects.equals(emotionCode, id.emotionCode);
    }

    @Override public int hashCode() { return Objects.hash(analysisId, emotionCode); }
}
