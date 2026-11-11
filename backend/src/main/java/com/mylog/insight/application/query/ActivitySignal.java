package com.mylog.insight.application.query;

import java.time.LocalDate;

/** An activity code recorded in the user's check-in for a day. */
public record ActivitySignal(LocalDate date, String code) {}
