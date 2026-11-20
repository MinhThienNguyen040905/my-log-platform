package com.mylog.insight;

import com.mylog.analysis.application.ThinkingPatternTaxonomy;
import com.mylog.insight.application.ThinkingPatternTexts;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThinkingPatternTextsTest {
    /** Cards name habits of thought, never conditions, treatments or crisis services. */
    static final List<String> FORBIDDEN = List.of("chẩn đoán", "trầm cảm", "rối loạn", "bệnh", "thuốc", "điều trị",
            "hotline", "http", "tự tử", "tự hại", "bạn là người");

    @Test void everyTaxonomyCodeHasWordingStepsAndASafeNarrative() {
        for (String code : ThinkingPatternTaxonomy.CODES) {
            var text = ThinkingPatternTexts.of(code);
            assertFalse(text.name().isBlank(), code);
            assertEquals(3, ThinkingPatternTexts.steps(code).size(), code);
            String narrative = ThinkingPatternTexts.narrative(code, 4, 3);
            assertTrue(narrative.contains("4 bài") && narrative.contains("3 tuần"), code);
            assertTrue(narrative.endsWith("Điều này có đúng với bạn không?"), "hands the decision back: " + code);
            String all = (narrative + " " + String.join(" ", ThinkingPatternTexts.steps(code))).toLowerCase(Locale.ROOT);
            for (String word : FORBIDDEN) assertFalse(all.contains(word), code + " contains " + word);
        }
    }
}
