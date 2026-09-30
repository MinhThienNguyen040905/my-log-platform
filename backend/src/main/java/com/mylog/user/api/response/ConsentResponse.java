package com.mylog.user.api.response;

import com.mylog.user.application.query.ConsentView;

import java.time.Instant;

public record ConsentResponse(String type, String documentVersion, boolean granted, Instant decidedAt) {
    public static ConsentResponse from(ConsentView view) {
        return new ConsentResponse(view.type(), view.documentVersion(), view.granted(), view.decidedAt());
    }
}
