package com.mylog.journal.domain;

import java.util.List;
import java.util.Map;

/**
 * Pure, stateless selection of companion copy by mood and draft state. No user-written text is
 * ever echoed back; every string here is fixed and reviewed content, matching the non-generative
 * baseline described in the companion plan (stage 1, before any LLM adapter is approved).
 */
public final class CompanionMoodTemplates {
    private CompanionMoodTemplates() {}

    /** `suggestedTitle` is backend-owned reviewed copy, never derived from user text; null unless offered. */
    public record Variant(String reply, List<String> questions, List<CompanionActionCode> actions, String suggestedTitle) {
        public Variant(String reply, List<String> questions, List<CompanionActionCode> actions) {
            this(reply, questions, actions, null);
        }
    }

    private static final Variant HAPPY = new Variant(
            "Nghe như hôm nay có một khoảnh khắc đáng nhớ.",
            List.of("Lúc đó điều đầu tiên bạn nghĩ tới là gì?", "Bạn muốn giữ lại cảm giác này bằng cách nào?"),
            List.of(), "Một điều nhỏ khiến mình mỉm cười hôm nay");

    private static final Variant NEUTRAL = new Variant(
            "Chào bạn. Hôm nay trong lòng bạn đang có gì?",
            List.of("Có khoảnh khắc nhỏ nào hôm nay bạn vẫn còn nhớ?", "Điều gì hôm nay còn hơi lấn cấn trong bạn?"),
            List.of(), "Vài dòng nhìn lại nhịp sống hôm nay");

    private static final Variant ANGRY = new Variant(
            "Nghe như có chuyện làm bạn bức bối. Bạn muốn hạ nhiệt trước, hay xả hết ra trang viết?",
            List.of("Chuyện gì chạm vào bạn nhiều nhất?", "Lúc này bạn cần điều gì nhất?"),
            List.of(CompanionActionCode.BREATHING_2MIN, CompanionActionCode.VENT_PROMPT));

    private static final Variant SAD = new Variant(
            "Nghe thật sự nặng lòng. Mình ở đây.",
            List.of("Bạn có muốn kể thêm một chút về chuyện này không?",
                    "Điều gì cứ lặp đi lặp lại trong suy nghĩ của bạn?",
                    "Lần gần nhất thấy nhẹ hơn, bạn đã làm gì?"),
            List.of(CompanionActionCode.AMBIENT, CompanionActionCode.BREATHING_2MIN));

    private static final Variant VERY_BAD = new Variant(
            "Mình ở đây với bạn. Mình thở chậm cùng nhau một chút trước nhé?",
            List.of("Nếu muốn, bạn có thể viết một câu về điều đang dồn lại."),
            List.of(CompanionActionCode.BREATHING_2MIN, CompanionActionCode.AMBIENT));

    private static final Variant EMPTY_DRAFT = new Variant(
            "Trang vẫn còn trống. Bạn muốn bắt đầu từ đâu?",
            List.of("Điều gì hôm nay khiến bạn muốn mở trang viết này?"),
            List.of());

    private static final Variant LONG_DRAFT = new Variant(
            "Bạn đã viết được khá nhiều rồi đấy. Đang kẹt ý ở đoạn này, hay muốn dừng lại thở một nhịp?",
            List.of("Đoạn vừa viết, điều gì bạn muốn khai thác thêm?"),
            List.of(CompanionActionCode.BREATHING_2MIN));

    private static final Map<String, Variant> BY_MOOD = Map.of(
            "happy", HAPPY,
            "neutral", NEUTRAL,
            "angry", ANGRY,
            "sad", SAD,
            "very_bad", VERY_BAD);

    private static final Map<String, Variant> TOOL_COMPLETED_FOLLOWUP = Map.of(
            "very_bad", new Variant("Cứ thong thả nhé. Mình vẫn ở đây nếu bạn cần.", List.of(), List.of()));

    private static final Variant TOOL_COMPLETED_DEFAULT =
            new Variant("Bạn thấy dịu hơn chút nào chưa?", List.of(), List.of());

    private static final Variant CLOSING_ACK =
            new Variant("Cảm ơn bạn đã chia sẻ cùng mình hôm nay.", List.of(), List.of());

    private static final Variant GENERIC_FOLLOW_UP = new Variant(
            "Mình nghe rồi. Cảm ơn bạn đã kể thêm cho mình.",
            List.of("Bạn muốn nói thêm điều gì nữa không?"),
            List.of());

    /** Picks the opening variant for the `OPEN` intent: mood in distress always wins over draft state. */
    public static Variant opening(String moodCode, boolean draftEmpty, boolean draftLong) {
        String mood = normalize(moodCode);
        if ("sad".equals(mood) || "very_bad".equals(mood) || "angry".equals(mood)) {
            return BY_MOOD.get(mood);
        }
        if (draftEmpty) return EMPTY_DRAFT;
        if (draftLong) return LONG_DRAFT;
        return BY_MOOD.getOrDefault(mood, NEUTRAL);
    }

    public static Variant freeTextFollowUp(String moodCode) {
        String mood = normalize(moodCode);
        Variant moodVariant = BY_MOOD.get(mood);
        if (moodVariant == null) return GENERIC_FOLLOW_UP;
        List<String> questions = moodVariant.questions().isEmpty()
                ? GENERIC_FOLLOW_UP.questions()
                : List.of(moodVariant.questions().get(moodVariant.questions().size() - 1));
        return new Variant(GENERIC_FOLLOW_UP.reply(), questions, List.of());
    }

    public static Variant toolCompleted(String moodCode) {
        return TOOL_COMPLETED_FOLLOWUP.getOrDefault(normalize(moodCode), TOOL_COMPLETED_DEFAULT);
    }

    public static Variant closing() {
        return CLOSING_ACK;
    }

    public static boolean offersLongTermChip(String moodCode) {
        return !"very_bad".equals(normalize(moodCode));
    }

    private static String normalize(String moodCode) {
        return moodCode == null ? "neutral" : moodCode.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
