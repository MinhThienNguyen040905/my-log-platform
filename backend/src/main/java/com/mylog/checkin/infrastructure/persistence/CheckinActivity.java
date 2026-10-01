package com.mylog.checkin.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checkin_activities")
class CheckinActivity {
    @Id UUID id;
    @Column(name = "checkin_id", nullable = false) UUID checkinId;
    @Column(name = "activity_code", nullable = false) String activityCode;
    @Column(name = "duration_minutes") Short durationMinutes;
    String intensity;
    @Column(name = "created_at", nullable = false) Instant createdAt;
}
