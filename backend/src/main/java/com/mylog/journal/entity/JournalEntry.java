package com.mylog.journal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "journal_entries")
public class JournalEntry {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(length = 200)
    private String title;

    @Column(name = "content_text", nullable = false, columnDefinition = "text")
    private String contentText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json", columnDefinition = "jsonb")
    private Map<String, Object> contentJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_format", nullable = false, length = 20)
    private ContentFormat contentFormat;

    @Column(name = "mood_score", nullable = false)
    private short moodScore;

    @Column(name = "stress_score")
    private Short stressScore;

    @Column(name = "energy_score")
    private Short energyScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JournalStatus status;

    @Column(name = "journal_version", nullable = false)
    private long journalVersion;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "timezone_at_entry", nullable = false, length = 64)
    private String timezoneAtEntry;

    @Column(name = "is_favorite", nullable = false)
    private boolean favorite;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected JournalEntry() {}

    public JournalEntry(
            UUID id,
            UUID userId,
            String title,
            String contentText,
            Map<String, Object> contentJson,
            ContentFormat contentFormat,
            short moodScore,
            Short stressScore,
            Short energyScore,
            Instant occurredAt,
            LocalDate entryDate,
            String timezoneAtEntry,
            boolean favorite,
            Instant now) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.contentText = contentText;
        this.contentJson = contentJson;
        this.contentFormat = contentFormat;
        this.moodScore = moodScore;
        this.stressScore = stressScore;
        this.energyScore = energyScore;
        this.status = JournalStatus.SAVED;
        this.journalVersion = 1;
        this.occurredAt = occurredAt;
        this.entryDate = entryDate;
        this.timezoneAtEntry = timezoneAtEntry;
        this.favorite = favorite;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean update(
            String title,
            String contentText,
            Map<String, Object> contentJson,
            ContentFormat contentFormat,
            short moodScore,
            Short stressScore,
            Short energyScore,
            Instant occurredAt,
            LocalDate entryDate,
            String timezoneAtEntry,
            boolean favorite,
            Instant now) {
        boolean analysisInputChanged = !Objects.equals(this.contentText, contentText)
                || !Objects.equals(this.contentJson, contentJson)
                || this.contentFormat != contentFormat
                || this.moodScore != moodScore
                || !Objects.equals(this.stressScore, stressScore)
                || !Objects.equals(this.energyScore, energyScore)
                || !Objects.equals(this.occurredAt, occurredAt)
                || !Objects.equals(this.entryDate, entryDate)
                || !Objects.equals(this.timezoneAtEntry, timezoneAtEntry);

        this.title = title;
        this.contentText = contentText;
        this.contentJson = contentJson;
        this.contentFormat = contentFormat;
        this.moodScore = moodScore;
        this.stressScore = stressScore;
        this.energyScore = energyScore;
        this.occurredAt = occurredAt;
        this.entryDate = entryDate;
        this.timezoneAtEntry = timezoneAtEntry;
        this.favorite = favorite;
        this.updatedAt = now;

        if (analysisInputChanged) {
            journalVersion++;
            status = status == JournalStatus.ANALYZED
                    ? JournalStatus.ANALYSIS_OUTDATED
                    : JournalStatus.SAVED;
        }
        return analysisInputChanged;
    }

    public void softDelete(Instant now) {
        deletedAt = now;
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getContentText() { return contentText; }
    public Map<String, Object> getContentJson() { return contentJson; }
    public ContentFormat getContentFormat() { return contentFormat; }
    public short getMoodScore() { return moodScore; }
    public Short getStressScore() { return stressScore; }
    public Short getEnergyScore() { return energyScore; }
    public JournalStatus getStatus() { return status; }
    public long getJournalVersion() { return journalVersion; }
    public Instant getOccurredAt() { return occurredAt; }
    public LocalDate getEntryDate() { return entryDate; }
    public String getTimezoneAtEntry() { return timezoneAtEntry; }
    public boolean isFavorite() { return favorite; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
