package com.mylog.journal.infrastructure;

import com.mylog.journal.application.WritingSuggestionGenerator;

final class TemplateWritingSuggestionGenerator implements WritingSuggestionGenerator {
    private static final String PROMPT_VERSION = "writing-prompts-draft-v1";
    private static final String[] PROMPTS = {
            "Điều gì trong khoảnh khắc đó khiến bạn chú ý nhất?",
            "Bạn muốn kể thêm điều gì về trải nghiệm này?",
            "Khi nhìn lại, điều gì bạn muốn ghi nhớ về hôm nay?"
    };

    @Override public Suggestion generate(String text) {
        return new Suggestion(PROMPTS[Math.floorMod(text.length() / 40, PROMPTS.length)], PROMPT_VERSION);
    }
}
