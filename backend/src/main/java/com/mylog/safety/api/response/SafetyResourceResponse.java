package com.mylog.safety.api.response;

import com.mylog.safety.application.query.SafetyResourceView;

import java.util.UUID;

public record SafetyResourceResponse(UUID id, String name, String resourceType, String contactValue,
                                     String description, String sourceUrl, int version) {
    public static SafetyResourceResponse from(SafetyResourceView view) {
        return new SafetyResourceResponse(view.id(), view.name(), view.resourceType(), view.contactValue(),
                view.description(), view.sourceUrl(), view.version());
    }
}
