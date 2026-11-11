package com.mylog.insight.application;

import com.mylog.insight.domain.PatternDetectors;
import com.mylog.insight.domain.PatternFinding;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reviewed Vietnamese sentence templates for pattern insights. They are the default narrative and the
 * fallback whenever AI wording is disabled, not consented, unavailable or rejected. Templates describe
 * what was observed and never claim causes or name conditions.
 */
public final class InsightTemplates {
    /** Stand-in for a user-written tag name in anything sent to an AI provider. */
    public static final String TAG_PLACEHOLDER = "[THẺ]";

    private InsightTemplates() {}

    public static String render(PatternFinding finding, String tagLabel) {
        Map<String, Object> f = finding.facts();
        boolean up = "UP".equals(finding.direction());
        return switch (finding.type()) {
            case PatternDetectors.WEEKDAY -> "Điểm " + metric(f.get("metric")) + " của bạn thường "
                    + (up ? "cao" : "thấp") + " hơn vào " + weekday(f.get("weekday")) + ": trung bình "
                    + num(f.get("dayAverage")) + " so với mức chung " + num(f.get("overallAverage"))
                    + ". Dựa trên " + finding.sampleSize() + " ngày có dữ liệu.";
            case PatternDetectors.TREND -> up
                    ? "Nửa tháng gần đây điểm tâm trạng của bạn nhích lên: trung bình " + num(f.get("secondAverage"))
                      + " so với " + num(f.get("firstAverage")) + " ở nửa tháng trước."
                    : "Nửa tháng gần đây điểm tâm trạng của bạn thấp hơn nửa tháng trước ("
                      + num(f.get("secondAverage")) + " so với " + num(f.get("firstAverage"))
                      + "). Ghi lại điều gì đang chiếm nhiều năng lượng có thể giúp bạn nhìn rõ hơn.";
            case PatternDetectors.EMOTION_TOPIC -> "Khi viết về " + topic(f.get("topic")) + ", sắc thái "
                    + emotion(f.get("emotion")) + " xuất hiện trong " + f.get("percentWithTopic")
                    + "% số bài, nhiều hơn mức chung " + f.get("percentOverall") + "%. Dựa trên "
                    + finding.sampleSize() + " bài đã phân tích.";
            case PatternDetectors.TAG_MOOD -> "Những bài gắn thẻ \"" + tagLabel + "\" thường có điểm tâm trạng "
                    + (up ? "cao" : "thấp") + " hơn: " + num(f.get("withAverage")) + " so với "
                    + num(f.get("withoutAverage")) + " ở các bài còn lại. Đây là điều đi cùng nhau, không phải nguyên nhân.";
            case PatternDetectors.ACTIVITY_MOOD -> "Những ngày có " + activity(f.get("activity"))
                    + ", điểm tâm trạng của bạn thường " + (up ? "cao" : "thấp") + " hơn: "
                    + num(f.get("withAverage")) + " so với " + num(f.get("withoutAverage"))
                    + " ở những ngày khác. Đây là điều đi cùng nhau, không phải nguyên nhân.";
            case PatternDetectors.SLEEP_MOOD -> f.containsKey("shortSleepMood")
                    ? "Những ngày ngủ dưới 6 tiếng, điểm tâm trạng trung bình của bạn là " + num(f.get("shortSleepMood"))
                      + ", so với " + num(f.get("longerSleepMood")) + " khi ngủ nhiều hơn. Đây là mối liên hệ quan sát được, không phải nguyên nhân."
                    : "Trong " + finding.sampleSize() + " ngày gần đây, thời lượng ngủ và điểm tâm trạng của bạn có xu hướng đi "
                      + (up ? "cùng" : "ngược") + " chiều. Đây là mối liên hệ quan sát được, không phải nguyên nhân.";
            default -> throw new IllegalArgumentException("Unknown insight type");
        };
    }

    /** Human-readable facts for the AI wording step: labels instead of codes, no user-written text. */
    public static Map<String, Object> readableFacts(PatternFinding finding) {
        Map<String, Object> out = new LinkedHashMap<>();
        finding.facts().forEach((key, value) -> {
            switch (key) {
                case "metric" -> out.put(key, metric(value));
                case "weekday" -> out.put(key, weekday(value));
                case "topic" -> out.put(key, topic(value));
                case "emotion" -> out.put(key, emotion(value));
                case "activity" -> out.put(key, activity(value));
                case "tagId" -> out.put("tag", TAG_PLACEHOLDER);
                default -> out.put(key, value instanceof BigDecimal d ? num(d) : value);
            }
        });
        out.put("sampleSize", finding.sampleSize());
        return out;
    }

    static String num(Object value) {
        if (value == null) return "";
        BigDecimal d = value instanceof BigDecimal b ? b : new BigDecimal(value.toString());
        return d.stripTrailingZeros().scale() <= 0 ? d.toBigInteger().toString() : d.toPlainString().replace('.', ',');
    }

    static String metric(Object code) {
        return "STRESS".equals(code) ? "căng thẳng" : "tâm trạng";
    }

    static String weekday(Object code) {
        return switch (String.valueOf(code)) {
            case "MONDAY" -> "thứ Hai";
            case "TUESDAY" -> "thứ Ba";
            case "WEDNESDAY" -> "thứ Tư";
            case "THURSDAY" -> "thứ Năm";
            case "FRIDAY" -> "thứ Sáu";
            case "SATURDAY" -> "thứ Bảy";
            default -> "Chủ nhật";
        };
    }

    static String emotion(Object code) {
        return switch (String.valueOf(code)) {
            case "JOY" -> "vui vẻ";
            case "SADNESS" -> "buồn";
            case "ANGER" -> "bực bội";
            case "FEAR" -> "sợ hãi";
            case "CALM" -> "bình yên";
            case "HOPE" -> "hy vọng";
            case "ANXIETY" -> "lo lắng";
            default -> "khác";
        };
    }

    static String topic(Object code) {
        return switch (String.valueOf(code)) {
            case "WORK" -> "công việc";
            case "RELATIONSHIPS" -> "các mối quan hệ";
            case "HEALTH" -> "sức khỏe";
            case "SLEEP" -> "giấc ngủ";
            case "SELF_CARE" -> "chăm sóc bản thân";
            default -> "chủ đề khác";
        };
    }

    static String activity(Object code) {
        String value = String.valueOf(code);
        return switch (value) {
            case "walking" -> "đi bộ";
            case "running" -> "chạy bộ";
            case "stretching" -> "giãn cơ";
            case "exercise" -> "tập thể dục";
            case "reading" -> "đọc sách";
            case "cooking" -> "nấu ăn";
            case "meditation" -> "thiền";
            case "journaling" -> "viết nhật ký";
            case "studying" -> "học tập";
            case "social" -> "gặp gỡ bạn bè";
            case "eating-out" -> "đi ăn ngoài";
            case "family-time" -> "thời gian bên gia đình";
            case "housework" -> "làm việc nhà";
            case "nap" -> "ngủ trưa";
            case "meeting" -> "nhiều cuộc họp";
            case "work-overtime" -> "làm thêm giờ";
            default -> "hoạt động \"" + value.replace('-', ' ') + "\"";
        };
    }
}
