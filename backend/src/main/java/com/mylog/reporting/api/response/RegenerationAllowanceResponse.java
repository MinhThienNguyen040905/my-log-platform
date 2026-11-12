package com.mylog.reporting.api.response;

import com.mylog.reporting.application.ReportService;

import java.time.LocalDate;

/** {@code availableFrom}: today when available, otherwise next Monday in the user's timezone. */
public record RegenerationAllowanceResponse(boolean available, LocalDate availableFrom) {
    public static RegenerationAllowanceResponse from(ReportService.RegenerationAllowance allowance) {
        return new RegenerationAllowanceResponse(allowance.available(), allowance.availableFrom());
    }
}
