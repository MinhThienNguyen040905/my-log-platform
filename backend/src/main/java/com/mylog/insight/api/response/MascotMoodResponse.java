package com.mylog.insight.api.response;

import com.mylog.insight.application.query.MascotMoodView;

import java.time.LocalDate;

public record MascotMoodResponse(String mood, int moodCount, int entryCount, LocalDate from, LocalDate to, boolean calm) {
    public static MascotMoodResponse from(MascotMoodView view) {
        return new MascotMoodResponse(view.mood(), view.moodCount(), view.entryCount(), view.from(), view.to(), view.calm());
    }
}
