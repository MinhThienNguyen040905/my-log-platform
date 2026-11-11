package com.mylog.insight.application;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks AI-reworded insight sentences before they replace the template. Rejects anything that invents
 * numbers, links or contact details, uses diagnostic/medical language, or states a cause. This is a
 * baseline guard; outbound safety screening still runs afterwards.
 */
public final class InsightNarrativeValidator {
    static final int MAX_LENGTH = 420;
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)?");
    private static final Pattern PHONE_OR_LONG_DIGITS = Pattern.compile("\\d[\\d .-]{6,}\\d");
    private static final List<String> FORBIDDEN = List.of(
            "chẩn đoán", "trầm cảm", "rối loạn", "bệnh", "thuốc", "điều trị", "tự tử", "tự hại", "tự sát",
            "hotline", "đường dây nóng", "http", "www.", "@",
            "nguyên nhân", "khiến bạn", "gây ra", "bởi vì", "là do", "vì vậy nên", "chắc chắn là",
            "diagnos", "depress", "disorder", "suicid");
    private static final Pattern CAUSAL_VI = Pattern.compile("(^|[\\s,(])vì([\\s,]|$)");

    private InsightNarrativeValidator() {}

    public static boolean isAcceptable(String narrative, String draft, boolean requiresTagPlaceholder) {
        if (narrative == null) return false;
        String text = narrative.strip();
        if (text.isEmpty() || text.length() > MAX_LENGTH || text.contains("\n\n")) return false;
        // The templates' own disclaimer is allowed; any other mention of a cause is not.
        String lower = text.toLowerCase(Locale.ROOT).replace("không phải nguyên nhân", "");
        for (String word : FORBIDDEN) if (lower.contains(word)) return false;
        if (CAUSAL_VI.matcher(lower).find() || PHONE_OR_LONG_DIGITS.matcher(text).find()) return false;
        if (!numbersOf(draft).containsAll(numbersOf(text))) return false;
        int placeholders = occurrences(text, InsightTemplates.TAG_PLACEHOLDER);
        if (requiresTagPlaceholder ? placeholders != 1 : placeholders != 0) return false;
        String withoutPlaceholder = text.replace(InsightTemplates.TAG_PLACEHOLDER, "");
        return withoutPlaceholder.indexOf('[') < 0 && withoutPlaceholder.indexOf(']') < 0
                && withoutPlaceholder.indexOf('<') < 0 && withoutPlaceholder.indexOf('>') < 0;
    }

    /** Numbers compared by value, so "6" and "6,0" (or "6.0") count as the same number. */
    private static Set<java.math.BigDecimal> numbersOf(String text) {
        Set<java.math.BigDecimal> out = new HashSet<>();
        if (text == null) return out;
        Matcher m = NUMBER.matcher(text);
        while (m.find()) out.add(new java.math.BigDecimal(m.group().replace(',', '.')).stripTrailingZeros());
        return out;
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) count++;
        return count;
    }
}
