package com.mylog.analysis.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Accepts only extractor output that can be trusted to show the user: a known code, a confidence in [0, 1]
 * and a quote that appears word for word in the entry, so a model can never put words in the user's mouth.
 * Invalid signals are dropped one by one; the rest are kept (best per code, at most {@link #MAX_SIGNALS}).
 */
public final class PatternSignalValidator {
    public static final int MAX_SIGNALS = 5;
    public static final int MAX_QUOTE_LENGTH = 160;
    static final int MIN_QUOTE_LENGTH = 8;
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final String WRAPPING_QUOTES = "\"'“”‘’«»";

    private PatternSignalValidator() {}

    public static List<PatternExtractor.Signal> accept(List<PatternExtractor.Signal> raw, String title, String plainText) {
        if (raw == null || raw.isEmpty()) return List.of();
        String source = normalize((title == null ? "" : title) + "\n" + (plainText == null ? "" : plainText));
        Map<String, PatternExtractor.Signal> best = new LinkedHashMap<>();
        for (PatternExtractor.Signal signal : raw) {
            if (signal == null || !ThinkingPatternTaxonomy.isKnown(signal.code())) continue;
            BigDecimal confidence = signal.confidence();
            if (confidence == null || confidence.signum() < 0 || confidence.compareTo(BigDecimal.ONE) > 0) continue;
            String quote = unwrap(normalize(signal.quote()));
            if (quote.length() < MIN_QUOTE_LENGTH || !source.contains(quote)) continue;
            var accepted = new PatternExtractor.Signal(signal.code(), confidence.setScale(4, RoundingMode.HALF_UP),
                    shorten(quote));
            var previous = best.get(signal.code());
            if (previous == null || accepted.confidence().compareTo(previous.confidence()) > 0)
                best.put(signal.code(), accepted);
        }
        List<PatternExtractor.Signal> out = new ArrayList<>(best.values());
        out.sort(Comparator.comparing(PatternExtractor.Signal::confidence).reversed()
                .thenComparing(PatternExtractor.Signal::code));
        return out.size() > MAX_SIGNALS ? List.copyOf(out.subList(0, MAX_SIGNALS)) : List.copyOf(out);
    }

    static String normalize(String text) {
        return text == null ? "" : SPACES.matcher(text).replaceAll(" ").strip();
    }

    private static String unwrap(String quote) {
        String out = quote;
        while (out.length() >= 2 && WRAPPING_QUOTES.indexOf(out.charAt(0)) >= 0
                && WRAPPING_QUOTES.indexOf(out.charAt(out.length() - 1)) >= 0)
            out = out.substring(1, out.length() - 1).strip();
        return out;
    }

    private static String shorten(String quote) {
        return quote.length() <= MAX_QUOTE_LENGTH ? quote : quote.substring(0, MAX_QUOTE_LENGTH - 1).stripTrailing() + "…";
    }
}
