package com.mylog.journal.api;

import com.mylog.journal.domain.ContentFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;

public class UpdateJournalRequest {

    @NotNull
    @Min(0)
    private Long version;

    @Size(max = 200)
    private String title;

    @Size(max = 100_000)
    private String contentText;

    private Map<String, Object> contentJson;
    private ContentFormat contentFormat;

    @Min(1)
    @Max(10)
    private Integer moodScore;

    @Min(1)
    @Max(10)
    private Integer stressScore;

    @Min(1)
    @Max(10)
    private Integer energyScore;

    private Instant occurredAt;

    @Size(max = 64)
    private String timezoneAtEntry;

    private Boolean favorite;

    private boolean titleSet;
    private boolean contentTextSet;
    private boolean contentJsonSet;
    private boolean contentFormatSet;
    private boolean moodScoreSet;
    private boolean stressScoreSet;
    private boolean energyScoreSet;
    private boolean occurredAtSet;
    private boolean timezoneAtEntrySet;
    private boolean favoriteSet;

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; this.titleSet = true; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; this.contentTextSet = true; }
    public Map<String, Object> getContentJson() { return contentJson; }
    public void setContentJson(Map<String, Object> contentJson) { this.contentJson = contentJson; this.contentJsonSet = true; }
    public ContentFormat getContentFormat() { return contentFormat; }
    public void setContentFormat(ContentFormat contentFormat) { this.contentFormat = contentFormat; this.contentFormatSet = true; }
    public Integer getMoodScore() { return moodScore; }
    public void setMoodScore(Integer moodScore) { this.moodScore = moodScore; this.moodScoreSet = true; }
    public Integer getStressScore() { return stressScore; }
    public void setStressScore(Integer stressScore) { this.stressScore = stressScore; this.stressScoreSet = true; }
    public Integer getEnergyScore() { return energyScore; }
    public void setEnergyScore(Integer energyScore) { this.energyScore = energyScore; this.energyScoreSet = true; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; this.occurredAtSet = true; }
    public String getTimezoneAtEntry() { return timezoneAtEntry; }
    public void setTimezoneAtEntry(String timezoneAtEntry) { this.timezoneAtEntry = timezoneAtEntry; this.timezoneAtEntrySet = true; }
    public Boolean getFavorite() { return favorite; }
    public void setFavorite(Boolean favorite) { this.favorite = favorite; this.favoriteSet = true; }

    public boolean titleSupplied() { return titleSet; }
    public boolean contentTextSupplied() { return contentTextSet; }
    public boolean contentJsonSupplied() { return contentJsonSet; }
    public boolean contentFormatSupplied() { return contentFormatSet; }
    public boolean moodScoreSupplied() { return moodScoreSet; }
    public boolean stressScoreSupplied() { return stressScoreSet; }
    public boolean energyScoreSupplied() { return energyScoreSet; }
    public boolean occurredAtSupplied() { return occurredAtSet; }
    public boolean timezoneAtEntrySupplied() { return timezoneAtEntrySet; }
    public boolean favoriteSupplied() { return favoriteSet; }

    @AssertTrue(message = "At least one journal field must be provided")
    @Schema(hidden = true)
    public boolean isMutationPresent() {
        return titleSet || contentTextSet || contentJsonSet || contentFormatSet || moodScoreSet
                || stressScoreSet || energyScoreSet || occurredAtSet || timezoneAtEntrySet || favoriteSet;
    }
}
