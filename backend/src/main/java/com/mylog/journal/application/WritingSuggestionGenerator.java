package com.mylog.journal.application;

public interface WritingSuggestionGenerator {
    Suggestion generate(String text);

    record Suggestion(String question, String promptVersion) {}
}
