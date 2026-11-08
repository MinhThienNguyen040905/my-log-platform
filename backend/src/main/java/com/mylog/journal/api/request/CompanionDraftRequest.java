package com.mylog.journal.api.request;

import jakarta.validation.constraints.Size;

/** The current draft, sent fresh on every turn so the companion reads the latest writing. Never stored. */
public record CompanionDraftRequest(@Size(max = 200) String title, @Size(max = 4000) String text,
                                    Integer wordCount, boolean truncated) {}
