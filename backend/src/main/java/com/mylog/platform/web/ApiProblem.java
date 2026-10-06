package com.mylog.platform.web;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public record ApiProblem(
        URI type,
        String title,
        int status,
        String code,
        String detail,
        String instance,
        String requestId,
        Instant timestamp,
        List<FieldViolation> errors
) {
    public ApiProblem {
        errors = List.copyOf(errors);
    }
}
