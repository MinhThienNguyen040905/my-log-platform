package com.mylog.journal.api.response;

import com.mylog.journal.application.query.JournalTagView;

import java.util.UUID;

public record JournalTagResponse(UUID id, String name, String color) {
    public static JournalTagResponse from(JournalTagView view) {
        return new JournalTagResponse(view.id(), view.name(), view.color());
    }
}
