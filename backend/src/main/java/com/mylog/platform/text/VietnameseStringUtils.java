package com.mylog.platform.text;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class VietnameseStringUtils {
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private VietnameseStringUtils() {}

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String nfd = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(nfd)
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .toLowerCase(Locale.ROOT);
    }

    public static boolean matches(String text, String normalizedQuery) {
        if (text == null || normalizedQuery == null || normalizedQuery.isBlank()) {
            return false;
        }
        return normalize(text).contains(normalizedQuery);
    }
}
