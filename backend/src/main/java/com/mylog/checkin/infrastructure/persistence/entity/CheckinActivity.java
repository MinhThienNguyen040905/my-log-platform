package com.mylog.checkin.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checkin_activities")
public class CheckinActivity {
    @Id public UUID id;
    @Column(name = "checkin_id", nullable = false) public UUID checkinId;
    @Column(name = "activity_code", nullable = false) public String activityCode;
    @Column(name = "duration_minutes") public Short durationMinutes;
    public String intensity;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
}
