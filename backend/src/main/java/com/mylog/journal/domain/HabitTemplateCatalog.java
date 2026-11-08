package com.mylog.journal.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reviewed habit templates offered by the writing companion when the user has no matching active
 * habit yet (stage 5 priority 3). Pure domain catalogue; no framework or persistence dependencies.
 */
public final class HabitTemplateCatalog {
    public record Template(String templateCode, String category, String presetTitle,
                           BigDecimal presetTargetValue, String presetUnit, String presetToolCode,
                           String text) {}

    private static final Template EVENING_BREATHING = new Template(
            "EVENING_BREATHING", "MINDFULNESS", "Thở sâu 2 phút mỗi tối",
            BigDecimal.valueOf(2), "minutes", "BREATHING_2MIN",
            "Bạn có muốn thử duy trì thói quen “Thở sâu 2 phút mỗi tối” không?");

    private static final Template CALM_AUDIO_PAUSE = new Template(
            "CALM_AUDIO_PAUSE", "MINDFULNESS", "Nghe âm thanh dịu 5 phút khi căng thẳng",
            BigDecimal.valueOf(5), "minutes", "AMBIENT",
            "Bạn có muốn thử thói quen “Nghe âm thanh dịu 5 phút khi căng thẳng” không?");

    private static final Template GENTLE_WALK = new Template(
            "GENTLE_WALK", "EXERCISE", "Đi bộ thư giãn 15 phút",
            BigDecimal.valueOf(15), "minutes", null,
            "Bạn có muốn thử thói quen “Đi bộ thư giãn 15 phút” mỗi ngày không?");

    private static final Template SLEEP_BEFORE_23H = new Template(
            "SLEEP_BEFORE_23H", "SLEEP", "Lên giường nghỉ trước 23h",
            BigDecimal.ONE, "sessions", null,
            "Bạn có muốn thử thói quen “Lên giường nghỉ trước 23h” để cơ thể đỡ mệt hơn không?");

    private static final List<Template> ALL = List.of(
            EVENING_BREATHING, CALM_AUDIO_PAUSE, GENTLE_WALK, SLEEP_BEFORE_23H);

    private HabitTemplateCatalog() {}

    public static List<Template> all() {
        return ALL;
    }

    public static Template forContext(String completedTool, String moodCode) {
        if ("BREATHING_2MIN".equals(completedTool)) return EVENING_BREATHING;
        if ("AMBIENT".equals(completedTool)) return CALM_AUDIO_PAUSE;
        if ("sad".equalsIgnoreCase(moodCode)) return SLEEP_BEFORE_23H;
        if ("angry".equalsIgnoreCase(moodCode)) return EVENING_BREATHING;
        return GENTLE_WALK;
    }
}
