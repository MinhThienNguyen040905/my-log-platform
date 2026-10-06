package com.mylog.journal.api.request;

import jakarta.validation.constraints.NotBlank;

public record CreateJournalTagRequest(@NotBlank String name, String color) {}
