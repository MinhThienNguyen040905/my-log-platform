package com.mylog.journal.application;

import tools.jackson.databind.JsonNode;

public record JournalPayload(int schemaVersion, String title, JsonNode contentJson,
                             String plainText, String location) {}
