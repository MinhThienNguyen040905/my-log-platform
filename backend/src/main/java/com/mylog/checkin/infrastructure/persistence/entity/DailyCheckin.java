package com.mylog.checkin.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_checkins")
public class DailyCheckin {
    @Id public UUID id;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "local_date", nullable = false) public LocalDate localDate;
    @Column(nullable = false) public String timezone;
    @Column(name = "mood_code") public String moodCode;
    @Column(name = "mood_score") public BigDecimal moodScore;
    @Column(name = "stress_score") public BigDecimal stressScore;
    @Column(name = "energy_score") public BigDecimal energyScore;
    @Column(name = "sleep_minutes") public Short sleepMinutes;
    @Column(name = "encrypted_note") public byte[] encryptedNote;
    @Column(name = "note_iv") public byte[] noteIv;
    @Column(name = "note_wrapped_key") public byte[] noteWrappedKey;
    @Column(name = "note_key_version") public String noteKeyVersion;
    @Column(nullable = false) public String source;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "updated_at", nullable = false) public Instant updatedAt;
    @Column(name = "row_version", nullable = false) public long rowVersion;
}
