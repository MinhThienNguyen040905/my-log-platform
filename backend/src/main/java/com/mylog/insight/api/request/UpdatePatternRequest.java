package com.mylog.insight.api.request;

import jakarta.validation.constraints.Pattern;

/**
 * Either {@code feedback} (CONFIRMED or NOT_REALLY, with an optional {@code reason} for NOT_REALLY) or
 * {@code status: HIDDEN}, never both.
 */
public record UpdatePatternRequest(
        @Pattern(regexp = "CONFIRMED|NOT_REALLY") String feedback,
        @Pattern(regexp = "NOT_LIKE_ME|DONT_WANT_TO_SEE|WRONG_QUOTE") String reason,
        @Pattern(regexp = "HIDDEN") String status) {}
