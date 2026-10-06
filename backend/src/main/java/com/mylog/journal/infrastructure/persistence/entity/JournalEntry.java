package com.mylog.journal.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
public class JournalEntry {
    @Id public UUID id;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "encrypted_payload", nullable = false) public byte[] encryptedPayload;
    @Column(name = "payload_iv", nullable = false) public byte[] payloadIv;
    @Column(name = "wrapped_data_key", nullable = false) public byte[] wrappedDataKey;
    @Column(name = "encryption_key_version", nullable = false) public String encryptionKeyVersion;
    @Column(name = "content_format_version", nullable = false) public short contentFormatVersion;
    @Column(name = "occurred_at", nullable = false) public Instant occurredAt;
    @Column(name = "local_date", nullable = false) public LocalDate localDate;
    @Column(name = "timezone", nullable = false) public String timezone;
    @Column(name = "mood_code") public String moodCode;
    @Column(name = "mood_score") public BigDecimal moodScore;
    @Column(name = "stress_score") public BigDecimal stressScore;
    @Column(name = "energy_score") public BigDecimal energyScore;
    @Column(name = "sleep_minutes") public Short sleepMinutes;
    @Column(name = "favorite", nullable = false) public boolean favorite;
    @Column(name = "entry_status", nullable = false) public String entryStatus;
    @Column(name = "risk_level", nullable = false) public String riskLevel;
    @Column(name = "analysis_status", nullable = false) public String analysisStatus;
    @Column(name = "content_version", nullable = false) public int contentVersion;
    @Column(name = "latest_analysis_id") public UUID latestAnalysisId;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt;
    @Column(name = "deleted_at") public Instant deletedAt;
    @Column(name = "row_version", nullable = false) public long rowVersion;
}
