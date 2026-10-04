package com.mylog.analysis.infrastructure;

import com.mylog.analysis.application.JournalAnalyzer;

class UnavailableJournalAnalyzer implements JournalAnalyzer {
    @Override public Result analyze(String title, String plainText) {
        throw new ProviderUnavailableException();
    }

    static final class ProviderUnavailableException extends RuntimeException {}
}
