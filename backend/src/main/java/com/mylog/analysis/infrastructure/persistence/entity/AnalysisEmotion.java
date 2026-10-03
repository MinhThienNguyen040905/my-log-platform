package com.mylog.analysis.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "analysis_emotions")
@IdClass(AnalysisEmotionId.class)
public class AnalysisEmotion {
    @Id @Column(name = "analysis_id") public UUID analysisId;
    @Id @Column(name = "emotion_code") public String emotionCode;
    @Column(nullable = false) public BigDecimal score;
    @Column(nullable = false) public short rank;
}
