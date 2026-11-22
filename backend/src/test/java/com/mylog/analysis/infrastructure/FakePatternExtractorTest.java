package com.mylog.analysis.infrastructure;

import com.mylog.analysis.application.PatternSignalValidator;
import com.mylog.analysis.application.ThinkingPatternTaxonomy;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plumbing checks for the keyword extractor and the synthetic tp-v1 evaluation file. Not a quality measure. */
class FakePatternExtractorTest {
    private final FakePatternExtractor extractor = new FakePatternExtractor();

    @Test void quotesWholeSentencesThatPassTheValidator() {
        String text = "Hôm nay đi làm như mọi ngày. Lỡ dự án trễ thêm một tuần nữa thì coi như sự nghiệp tiêu tan. "
                + "Cả tối mình cứ tua lại câu nói vụng về với chị trưởng phòng.";
        var result = extractor.extract("Một ngày", text);
        var accepted = PatternSignalValidator.accept(result.signals(), "Một ngày", text);
        assertEquals(List.of("CATASTROPHIZING", "RUMINATION"), accepted.stream().map(s -> s.code()).sorted().toList());
        assertTrue(accepted.stream().allMatch(s -> text.contains(s.quote())));
    }

    @Test void evaluationFileUsesOnlyKnownCodesAndUniqueIds() throws Exception {
        var lines = evalLines();
        assertTrue(lines.size() >= 300, "eval set present");
        var ids = new HashSet<String>();
        for (JsonNode line : lines) {
            assertTrue(ids.add(line.path("id").asText()), "unique id " + line.path("id").asText());
            for (JsonNode label : line.path("labels"))
                assertTrue(ThinkingPatternTaxonomy.isKnown(label.asText()), "known code " + label.asText());
            if (line.has("confusableWith"))
                assertTrue(ThinkingPatternTaxonomy.isKnown(line.path("confusableWith").asText()));
        }
    }

    @Test void keywordRulesStayQuietOnNeutralSentences() throws Exception {
        List<String> noisy = new ArrayList<>();
        for (JsonNode line : evalLines()) {
            if (!"NEUTRAL".equals(line.path("kind").asText())) continue;
            String text = line.path("text").asText();
            if (!extractor.extract("", text).signals().isEmpty()) noisy.add(line.path("id").asText());
        }
        assertEquals(List.of(), noisy);
    }

    static List<JsonNode> evalLines() throws Exception {
        var mapper = new ObjectMapper();
        try (var in = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                FakePatternExtractorTest.class.getResourceAsStream("/thinking-patterns/eval-tp-v1.jsonl")),
                StandardCharsets.UTF_8))) {
            List<JsonNode> out = new ArrayList<>();
            for (String line; (line = in.readLine()) != null; ) if (!line.isBlank()) out.add(mapper.readTree(line));
            return out;
        }
    }
}
