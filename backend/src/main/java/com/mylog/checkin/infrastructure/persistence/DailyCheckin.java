package com.mylog.checkin.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_checkins")
class DailyCheckin {
    @Id UUID id;
    @Column(name = "user_id", nullable = false) UUID userId;
    @Column(name = "local_date", nullable = false) LocalDate localDate;
    @Column(nullable = false) String timezone;
    @Column(name = "mood_code") String moodCode;
    @Column(name = "mood_score") BigDecimal moodScore;
    @Column(name = "stress_score") BigDecimal stressScore;
    @Column(name = "energy_score") BigDecimal energyScore;
    @Column(name = "sleep_minutes") Short sleepMinutes;
    @Column(name = "encrypted_note") byte[] encryptedNote;
    @Column(name = "note_iv") byte[] noteIv;
    @Column(name = "note_wrapped_key") byte[] noteWrappedKey;
    @Column(name = "note_key_version") String noteKeyVersion;
    @Column(nullable = false) String source;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Column(name = "row_version", nullable = false) long rowVersion;
}
