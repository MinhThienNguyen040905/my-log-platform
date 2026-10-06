package com.mylog.platform.observability;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataRedactorTest {

    @Test
    void redactsCredentialsIdentityAndJournalContent() {
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("Authorization", "Bearer secret");
        metadata.put("Cookie", "refresh=secret");
        metadata.put("email", "person@example.com");
        metadata.put("journalContent", "private text");
        metadata.put("provider_prompt", "private prompt");
        metadata.put("jobId", "job-123");
        metadata.put("attempt", 2);

        Map<String, Object> result = SensitiveDataRedactor.redact(metadata);

        assertThat(result)
                .containsEntry("Authorization", SensitiveDataRedactor.REDACTED)
                .containsEntry("Cookie", SensitiveDataRedactor.REDACTED)
                .containsEntry("email", SensitiveDataRedactor.REDACTED)
                .containsEntry("journalContent", SensitiveDataRedactor.REDACTED)
                .containsEntry("provider_prompt", SensitiveDataRedactor.REDACTED)
                .containsEntry("jobId", "job-123")
                .containsEntry("attempt", 2);
    }
}
