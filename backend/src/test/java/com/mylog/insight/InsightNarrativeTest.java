package com.mylog.insight;

import com.mylog.insight.application.InsightNarrativeValidator;
import com.mylog.insight.application.InsightNarrator;
import com.mylog.insight.application.InsightTemplates;
import com.mylog.insight.domain.ContrastDetectors;
import com.mylog.insight.domain.PatternDetectors;
import com.mylog.insight.domain.PatternFinding;
import com.mylog.insight.infrastructure.LlmInsightNarrator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Templates, validator and the AI wording adapter, with synthetic facts and a fake HTTP transport. */
class InsightNarrativeTest {
    static final String WEEKDAY_DRAFT =
            "Điểm căng thẳng của bạn thường cao hơn vào thứ Hai: trung bình 6,4 so với mức chung 4,9. Dựa trên 26 ngày có dữ liệu.";

    @Test void templatesUseVietnameseLabelsAndDecimalComma() {
        String weekday = InsightTemplates.render(weekday(), "");
        assertTrue(weekday.startsWith(WEEKDAY_DRAFT + " Thứ Hai của bạn"), weekday);
        assertTrue(weekday.endsWith("nhé."), "ends with a small experiment");
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("tagId", "00000000-0000-0000-0000-000000000001");
        facts.put("withAverage", new BigDecimal("7.8"));
        facts.put("withoutAverage", new BigDecimal("6.0"));
        var tag = new PatternFinding(PatternDetectors.TAG_MOOD, "UP", new BigDecimal("0.6"), 20, facts, List.of());
        String text = InsightTemplates.render(tag, "Bạn bè");
        assertTrue(text.contains("\"Bạn bè\"") && text.contains("7,8") && text.contains("không phải nguyên nhân"));
        assertEquals(InsightTemplates.TAG_PLACEHOLDER, InsightTemplates.readableFacts(tag).get("tag"));
        assertFalse(InsightTemplates.readableFacts(tag).containsKey("tagId"));
    }

    @Test void contrastTemplatesEndWithAQuestionAndUseNoCausalOrDiagnosticWords() {
        List<PatternFinding> findings = List.of(
                finding(ContrastDetectors.EXCEPTION, "UP", Map.of("conditionKind", "ACTIVITY", "conditionCode", "meeting",
                        "withAverage", new BigDecimal("4.8"), "withoutAverage", new BigDecimal("6.5"), "conditionDays", 6,
                        "exceptionDays", 3, "featureKind", "ACTIVITY", "featureCode", "walking", "featureDays", 2)),
                finding(ContrastDetectors.EXCEPTION, "UP", Map.of("conditionKind", "SHORT_SLEEP",
                        "withAverage", new BigDecimal("4.8"), "withoutAverage", new BigDecimal("6.5"), "conditionDays", 6,
                        "exceptionDays", 2)),
                finding(ContrastDetectors.CONDITION_SHIFT, "UP", Map.of("conditionKind", "TOPIC", "conditionCode", "WORK",
                        "nowWith", new BigDecimal("6.1"), "nowWithout", new BigDecimal("6.5"),
                        "prevWith", new BigDecimal("4.2"), "prevWithout", new BigDecimal("6.6"))),
                finding(ContrastDetectors.CONDITION_SHIFT, "DOWN", Map.of("conditionKind", "SHORT_SLEEP",
                        "nowWith", new BigDecimal("3.9"), "nowWithout", new BigDecimal("6.5"),
                        "prevWith", new BigDecimal("5.9"), "prevWithout", new BigDecimal("6.4"))),
                finding(ContrastDetectors.TOPIC_SHIFT, "UP", Map.of("topic", "RELATIONSHIPS", "nowCount", 9L,
                        "nowTotal", 20, "prevCount", 3L, "prevTotal", 18)),
                finding(ContrastDetectors.RECOVERY_SHIFT, "UP", Map.of("nowDays", new BigDecimal("1.5"),
                        "prevDays", new BigDecimal("3.0"))),
                finding(ContrastDetectors.RECOVERY_SHIFT, "DOWN", Map.of("nowDays", new BigDecimal("3.5"),
                        "prevDays", new BigDecimal("1.5"))));
        for (PatternFinding finding : findings) {
            String text = InsightTemplates.render(finding, "");
            assertTrue(text.endsWith("?") || text.endsWith("nhé.") || text.endsWith("nhé?"), text);
            assertTrue(InsightNarrativeValidator.isAcceptable(text, text, false), "template itself passes: " + text);
        }
        String exception = InsightTemplates.render(findings.getFirst(), "");
        assertTrue(exception.startsWith("Những ngày có nhiều cuộc họp, tâm trạng của bạn thường thấp hơn: 4,8 so với 6,5 ở những ngày khác."), exception);
        assertTrue(exception.contains("3 ngày như vậy bạn vẫn thấy ổn, và 2 ngày trong số đó có đi bộ"), exception);
        assertTrue(exception.contains("Có thể việc cơ thể được vận động và nghỉ ngơi là một điểm tựa của bạn"), exception);
        assertEquals("nhiều cuộc họp", InsightTemplates.readableFacts(findings.getFirst()).get("conditionCode"));
    }

    @Test void tagExceptionUsesThePlaceholderOnceForAi() {
        Map<String, Object> facts = new LinkedHashMap<>(Map.of("conditionKind", "TAG",
                "tagId", "00000000-0000-0000-0000-000000000001", "withAverage", new BigDecimal("4.8"),
                "withoutAverage", new BigDecimal("6.5"), "conditionDays", 6, "exceptionDays", 3,
                "featureKind", "TOPIC", "featureCode", "RELATIONSHIPS", "featureDays", 2));
        var finding = new PatternFinding(ContrastDetectors.EXCEPTION, "UP", new BigDecimal("0.6"), 20, facts, List.of());
        String draft = InsightTemplates.render(finding, InsightTemplates.TAG_PLACEHOLDER);
        assertTrue(InsightNarrativeValidator.isAcceptable(draft, draft, true), draft);
        assertTrue(InsightTemplates.render(finding, "Đi học").contains("\"Đi học\""));
        assertTrue(draft.contains("sự kết nối với người khác"), draft);
    }

    private static PatternFinding finding(String type, String direction, Map<String, Object> facts) {
        return new PatternFinding(type, direction, new BigDecimal("0.5"), 20, new LinkedHashMap<>(facts), List.of());
    }

    @Test void validatorAcceptsWarmRewordingWithSameNumbers() {
        assertTrue(InsightNarrativeValidator.isAcceptable(
                "Thứ Hai có vẻ là ngày nặng nhất tuần của bạn: căng thẳng trung bình 6,4, trong khi mức chung là 4,9. "
                        + "Thử để dành một khoảng nghỉ nhỏ vào sáng thứ Hai xem sao nhé.", WEEKDAY_DRAFT, false));
        assertTrue(InsightNarrativeValidator.isAcceptable("Mức chung là 6,0 còn hôm đó là 7,8.",
                "So với 6 thì 7,8 cao hơn.", false), "numbers are compared by value");
    }

    @Test void validatorRejectsInventedNumbersCausesDiagnosisAndContacts() {
        assertFalse(InsightNarrativeValidator.isAcceptable("Căng thẳng thứ Hai là 7,2.", WEEKDAY_DRAFT, false));
        assertFalse(InsightNarrativeValidator.isAcceptable("Thứ Hai bạn căng thẳng 6,4 vì công việc.", WEEKDAY_DRAFT, false));
        assertFalse(InsightNarrativeValidator.isAcceptable("Mức 6,4 có thể là dấu hiệu trầm cảm.", WEEKDAY_DRAFT, false));
        assertFalse(InsightNarrativeValidator.isAcceptable("Mức 6,4, hãy xem https://example.test.", WEEKDAY_DRAFT, false));
        assertFalse(InsightNarrativeValidator.isAcceptable("", WEEKDAY_DRAFT, false));
        assertFalse(InsightNarrativeValidator.isAcceptable("x".repeat(500), WEEKDAY_DRAFT, false));
    }

    @Test void validatorRequiresExactlyOneTagPlaceholderWhenNeeded() {
        String draft = "Những bài gắn thẻ \"[THẺ]\" thường có điểm tâm trạng cao hơn: 7,8 so với 6.";
        assertTrue(InsightNarrativeValidator.isAcceptable("Các bài gắn [THẺ] thường vui hơn: 7,8 so với 6.", draft, true));
        assertFalse(InsightNarrativeValidator.isAcceptable("Các bài về bạn bè thường vui hơn: 7,8 so với 6.", draft, true));
        assertFalse(InsightNarrativeValidator.isAcceptable("[THẺ] và [THẺ]: 7,8 so với 6.", draft, true));
    }

    @Test void llmNarratorSendsFactsOnlyAndParsesItems() {
        List<String> sent = new ArrayList<>();
        var narrator = new LlmInsightNarrator("https://provider.test/v1/chat/completions", "test-model", "synthetic-key",
                5000, JsonMapper.builder().build(), new BigDecimal("1"), new BigDecimal("2"), (uri, key, timeout, body) -> {
                    sent.add(body);
                    return new LlmInsightNarrator.HttpTransport.Response(200, """
                            {"choices":[{"finish_reason":"stop","message":{"content":"{\\"items\\":[{\\"key\\":\\"i1\\",\\"narrative\\":\\"Thứ Hai hơi nặng: 6,4 so với 4,9.\\"}]}"}}],
                             "usage":{"prompt_tokens":1000,"completion_tokens":500}}
                            """);
                });
        var result = narrator.narrate(List.of(new InsightNarrator.Request("i1", PatternDetectors.WEEKDAY, "UP",
                InsightTemplates.readableFacts(weekday()), WEEKDAY_DRAFT)));
        assertTrue(result.succeeded());
        assertEquals("Thứ Hai hơi nặng: 6,4 so với 4,9.", result.narratives().get("i1"));
        assertEquals(new BigDecimal("0.002000"), result.estimatedCostUsd());
        assertTrue(sent.getFirst().contains("\"store\":false"));
        assertTrue(sent.getFirst().contains("thứ Hai"));
    }

    @Test void llmNarratorFailsClosedOnProviderErrors() {
        var narrator = new LlmInsightNarrator("https://provider.test/v1/chat/completions", "test-model", "synthetic-key",
                5000, JsonMapper.builder().build(), BigDecimal.ZERO, BigDecimal.ZERO,
                (uri, key, timeout, body) -> new LlmInsightNarrator.HttpTransport.Response(500, "{}"));
        var result = narrator.narrate(List.of(new InsightNarrator.Request("i1", "X", "UP", Map.of(), WEEKDAY_DRAFT)));
        assertFalse(result.succeeded());
        assertTrue(result.narratives().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new LlmInsightNarrator("http://provider.test/v1", "m", "k",
                5000, JsonMapper.builder().build(), BigDecimal.ZERO, BigDecimal.ZERO));
    }

    static PatternFinding weekday() {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("metric", "STRESS");
        facts.put("weekday", "MONDAY");
        facts.put("dayAverage", new BigDecimal("6.4"));
        facts.put("overallAverage", new BigDecimal("4.9"));
        facts.put("daySamples", 4);
        return new PatternFinding(PatternDetectors.WEEKDAY, "UP", new BigDecimal("0.5"), 26, facts, List.of());
    }
}
