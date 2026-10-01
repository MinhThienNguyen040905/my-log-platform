package com.mylog.analysis.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_analyses")
public class AiAnalysis {
    @Id public UUID id;
    @Column(name = "journal_entry_id", nullable = false) public UUID journalEntryId;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "content_version", nullable = false) public int contentVersion;
    @Column(name = "analysis_version", nullable = false) public int analysisVersion;
    @Column(nullable = false) public String status;
    @Column(name = "sentiment_label") public String sentimentLabel;
    @Column(name = "sentiment_score") public BigDecimal sentimentScore;
    @Column(name = "encrypted_output") public byte[] encryptedOutput;
    @Column(name = "output_iv") public byte[] outputIv;
    @Column(name = "wrapped_data_key") public byte[] wrappedDataKey;
    @Column(name = "encryption_key_version") public String encryptionKeyVersion;
    @Column(name = "output_schema_version", nullable = false) public short outputSchemaVersion;
    @Column(nullable = false) public String provider;
    @Column(nullable = false) public String model;
    @Column(name = "model_version") public String modelVersion;
    @Column(name = "prompt_template_version", nullable = false) public String promptTemplateVersion;
    @Column(name = "safety_policy_version", nullable = false) public String safetyPolicyVersion;
    @Column(name = "started_at", nullable = false) public Instant startedAt;
    @Column(name = "completed_at") public Instant completedAt;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
}
