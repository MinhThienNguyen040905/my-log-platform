package com.mylog.journal.infrastructure.persistence;

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
class JournalEntry {
    @Id UUID id;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(name = "encrypted_payload", nullable = false) byte[] encryptedPayload;
    @Column(name = "payload_iv", nullable = false) byte[] payloadIv;
    @Column(name = "wrapped_data_key", nullable = false) byte[] wrappedDataKey;
    @Column(name = "encryption_key_version", nullable = false) String encryptionKeyVersion;
    @Column(name = "content_format_version", nullable = false) short contentFormatVersion;
    @Column(name = "occurred_at", nullable = false) Instant occurredAt;
    @Column(name = "local_date", nullable = false) LocalDate localDate;
    @Column(name = "timezone", nullable = false) String timezone;
    @Column(name = "mood_code") String moodCode;
    @Column(name = "mood_score") BigDecimal moodScore;
    @Column(name = "stress_score") BigDecimal stressScore;
    @Column(name = "energy_score") BigDecimal energyScore;
    @Column(name = "sleep_minutes") Short sleepMinutes;
    @Column(name = "favorite", nullable = false) boolean favorite;
    @Column(name = "entry_status", nullable = false) String entryStatus;
    @Column(name = "risk_level", nullable = false) String riskLevel;
    @Column(name = "analysis_status", nullable = false) String analysisStatus;
    @Column(name = "content_version", nullable = false) int contentVersion;
    @Column(name = "latest_analysis_id") UUID latestAnalysisId;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Column(name = "deleted_at") Instant deletedAt;
    @Column(name = "row_version", nullable = false) long rowVersion;
}
