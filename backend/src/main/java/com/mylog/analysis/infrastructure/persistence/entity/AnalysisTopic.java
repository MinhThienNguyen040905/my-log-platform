package com.mylog.analysis.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "analysis_topics")
public class AnalysisTopic {
    @Id public UUID id;
    @Column(name = "analysis_id", nullable = false) public UUID analysisId;
    @Column(name = "topic_code", nullable = false) public String topicCode;
    @Column(nullable = false) public BigDecimal score;
    @Column(nullable = false) public short rank;
}
