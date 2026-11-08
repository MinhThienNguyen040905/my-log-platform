package com.mylog.journal.domain;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fixed, reviewed title suggestions per mood for the companion's always-available "Gợi ý tựa đề"
 * button. Not AI-generated and never derived from user text, so it needs no consent and sends
 * nothing to a provider. Separate from {@code suggestedTitle} on a companion reply, which a future
 * LLM stage may fill from the draft.
 */
public final class TitleSuggestionCatalog {
    private TitleSuggestionCatalog() {}

    private static final Map<String, List<String>> BY_MOOD = Map.of(
            "happy", List.of(
                    "Một điều nhỏ khiến mình vui hôm nay",
                    "Khoảnh khắc hôm nay mình muốn giữ lại mãi",
                    "Lời cảm ơn gửi đến chính mình hôm nay"),
            "neutral", List.of(
                    "Vài dòng nhìn lại nhịp sống hôm nay",
                    "Điều mình đang học cách chấp nhận",
                    "Một suy nghĩ chợt đến lúc cuối ngày"),
            "angry", List.of(
                    "Tháo gỡ những bức bối trong lòng hôm nay",
                    "Điều mình thật sự cần lúc này để hạ nhiệt",
                    "Viết ra hết để ngày mai nhẹ đầu hơn"),
            "sad", List.of(
                    "Dịu dàng với bản thân trong một ngày trùng xuống",
                    "Điều đang làm lòng mình nặng trĩu hôm nay",
                    "Một việc nhỏ ấm áp mình muốn tự thưởng tối nay"),
            "very_bad", List.of(
                    "Cho phép mình nghỉ ngơi khi mọi thứ hơi quá tải",
                    "Gửi vài lời vỗ về cho chính mình lúc này",
                    "Thở chậm lại qua một ngày nhiều mệt mỏi"));

    /** Unknown or missing mood falls back to {@code neutral}. */
    public static List<String> forMood(String moodCode) {
        String mood = moodCode == null ? "neutral" : moodCode.trim().toLowerCase(Locale.ROOT);
        return BY_MOOD.getOrDefault(mood, BY_MOOD.get("neutral"));
    }
}
