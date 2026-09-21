package com.mylog.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_preferences")
public class UserPreference {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(name = "is_onboarded", nullable = false)
    private boolean onboarded;

    @Column(name = "preferred_journal_time")
    private LocalTime preferredJournalTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "journaling_goals", nullable = false, columnDefinition = "jsonb")
    private List<String> journalingGoals;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserPreference() {}

    public UserPreference(UUID userId, String timezone, String language, Instant now) {
        this.userId = userId;
        this.timezone = timezone;
        this.language = language;
        this.onboarded = false;
        this.journalingGoals = new ArrayList<>();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String timezone, String language, Instant now) {
        if (timezone != null) {
            this.timezone = timezone;
        }
        if (language != null) {
            this.language = language;
        }
        this.updatedAt = now;
    }

    public String getTimezone() { return timezone; }
    public String getLanguage() { return language; }
}
