package com.mylog.analysis.infrastructure;

import com.mylog.analysis.application.JournalAnalyzer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "mylog.ai", name = "fake-enabled", havingValue = "false", matchIfMissing = true)
class UnavailableJournalAnalyzer implements JournalAnalyzer {
    @Override public Result analyze(String title, String plainText) {
        throw new ProviderUnavailableException();
    }

    static final class ProviderUnavailableException extends RuntimeException {}
}
