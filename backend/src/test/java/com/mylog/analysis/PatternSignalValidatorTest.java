package com.mylog.analysis;

import com.mylog.analysis.application.PatternExtractor.Signal;
import com.mylog.analysis.application.PatternSignalValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Synthetic sentences only. */
class PatternSignalValidatorTest {
    static final String TITLE = "Một ngày dài";
    static final String TEXT = "Sáng nay họp nhóm.  Lỡ dự án trễ thêm một tuần nữa thì coi như\nsự nghiệp tiêu tan. "
            + "Tối về mình nấu canh chua.";

    @Test void keepsAQuoteThatAppearsWordForWordEvenAcrossLineBreaks() {
        var out = PatternSignalValidator.accept(List.of(signal("CATASTROPHIZING", "0.91",
                "Lỡ dự án trễ thêm một tuần nữa thì coi như sự nghiệp tiêu tan.")), TITLE, TEXT);
        assertEquals(1, out.size());
        assertEquals("Lỡ dự án trễ thêm một tuần nữa thì coi như sự nghiệp tiêu tan.", out.getFirst().quote());
        assertEquals(new BigDecimal("0.9100"), out.getFirst().confidence());
    }

    @Test void dropsParaphrasedOrInventedQuotes() {
        assertTrue(PatternSignalValidator.accept(List.of(
                signal("CATASTROPHIZING", "0.9", "Nếu dự án trễ thì sự nghiệp của mình tiêu tan."),
                signal("SELF_LABELING", "0.9", "Mình đúng là đồ vô dụng.")), TITLE, TEXT).isEmpty());
    }

    @Test void dropsUnknownCodesBadConfidenceAndTooShortQuotes() {
        assertTrue(PatternSignalValidator.accept(List.of(
                signal("DEPRESSION", "0.9", "Sáng nay họp nhóm."),
                signal("RUMINATION", "1.2", "Sáng nay họp nhóm."),
                signal("RUMINATION", "-0.1", "Sáng nay họp nhóm."),
                new Signal("RUMINATION", null, "Sáng nay họp nhóm."),
                signal("RUMINATION", "0.9", "họp"),
                signal("RUMINATION", "0.9", null)), TITLE, TEXT).isEmpty());
    }

    @Test void stripsWrappingQuotesAndKeepsTheBestSignalPerCode() {
        var out = PatternSignalValidator.accept(List.of(
                signal("RUMINATION", "0.6", "\"Sáng nay họp nhóm.\""),
                signal("RUMINATION", "0.8", "“Tối về mình nấu canh chua.”")), TITLE, TEXT);
        assertEquals(1, out.size());
        assertEquals("Tối về mình nấu canh chua.", out.getFirst().quote());
    }

    @Test void capsTheNumberOfSignalsAndShortensLongQuotes() {
        String longSentence = "Mình cứ nghĩ đi nghĩ lại " + "chuyện hôm qua ".repeat(20) + "mãi không thôi.";
        List<Signal> raw = new ArrayList<>();
        for (String code : List.of("CATASTROPHIZING", "ALL_OR_NOTHING", "OVERGENERALIZATION", "MIND_READING",
                "SHOULD_STATEMENTS", "SELF_LABELING", "RUMINATION"))
            raw.add(signal(code, "0.7", longSentence));
        var out = PatternSignalValidator.accept(raw, TITLE, longSentence);
        assertEquals(PatternSignalValidator.MAX_SIGNALS, out.size());
        out.forEach(s -> {
            assertEquals(PatternSignalValidator.MAX_QUOTE_LENGTH, s.quote().length());
            assertTrue(s.quote().endsWith("…"));
        });
    }

    private static Signal signal(String code, String confidence, String quote) {
        return new Signal(code, new BigDecimal(confidence), quote);
    }
}
