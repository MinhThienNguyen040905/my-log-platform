package com.mylog.insight.application;

import com.mylog.insight.domain.ContrastDetectors;
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

    /**
     * Each sentence states what was observed and ends with a question or a small experiment, so the user
     * reflects instead of reading a report. Ideas are offered as possibilities to check, never as labels.
     */
    public static String render(PatternFinding finding, String tagLabel) {
        Map<String, Object> f = finding.facts();
        boolean up = "UP".equals(finding.direction());
        return switch (finding.type()) {
            case PatternDetectors.WEEKDAY -> "Điểm " + metric(f.get("metric")) + " của bạn thường "
                    + (up ? "cao" : "thấp") + " hơn vào " + weekday(f.get("weekday")) + ": trung bình "
                    + num(f.get("dayAverage")) + " so với mức chung " + num(f.get("overallAverage"))
                    + ". Dựa trên " + finding.sampleSize() + " ngày có dữ liệu. " + capitalize(weekday(f.get("weekday")))
                    + " của bạn thường có gì khác những ngày còn lại? Tuần này thử để ý và ghi lại một điều nhé.";
            case PatternDetectors.TREND -> up
                    ? "Nửa tháng gần đây điểm tâm trạng của bạn nhích lên: trung bình " + num(f.get("secondAverage"))
                      + " so với " + num(f.get("firstAverage")) + " ở nửa tháng trước. Gần đây bạn đã làm gì khác đi?"
                      + " Ghi lại điều đó có thể giúp bạn giữ nó lâu hơn."
                    : "Nửa tháng gần đây điểm tâm trạng của bạn thấp hơn nửa tháng trước ("
                      + num(f.get("secondAverage")) + " so với " + num(f.get("firstAverage"))
                      + "). Ghi lại điều gì đang chiếm nhiều năng lượng có thể giúp bạn nhìn rõ hơn.";
            case PatternDetectors.EMOTION_TOPIC -> "Khi viết về " + topic(f.get("topic")) + ", sắc thái "
                    + emotion(f.get("emotion")) + " xuất hiện trong " + f.get("percentWithTopic")
                    + "% số bài, nhiều hơn mức chung " + f.get("percentOverall") + "%. Dựa trên "
                    + finding.sampleSize() + " bài đã phân tích. Lần tới khi viết về " + topic(f.get("topic"))
                    + ", thử hỏi mình: điều gì ở đó quan trọng với bạn nhất?";
            case PatternDetectors.TAG_MOOD -> "Những bài gắn thẻ \"" + tagLabel + "\" thường có điểm tâm trạng "
                    + (up ? "cao" : "thấp") + " hơn: " + num(f.get("withAverage")) + " so với "
                    + num(f.get("withoutAverage")) + " ở các bài còn lại. Đây là điều đi cùng nhau, không phải nguyên nhân. "
                    + (up ? "Điều gì trong những lúc ấy làm bạn thấy dễ chịu?"
                          : "Lần tới, thử ghi thêm một điều nhỏ giúp bạn dễ thở hơn trong những lúc ấy nhé.");
            case PatternDetectors.ACTIVITY_MOOD -> "Những ngày có " + activity(f.get("activity"))
                    + ", điểm tâm trạng của bạn thường " + (up ? "cao" : "thấp") + " hơn: "
                    + num(f.get("withAverage")) + " so với " + num(f.get("withoutAverage"))
                    + " ở những ngày khác. Đây là điều đi cùng nhau, không phải nguyên nhân. "
                    + (up ? "Tuần này bạn có muốn dành thêm một chút thời gian cho việc này không?"
                          : "Trong những ngày ấy, phần nào thấy nặng nhất với bạn?");
            case PatternDetectors.SLEEP_MOOD -> (f.containsKey("shortSleepMood")
                    ? "Những ngày ngủ dưới 6 tiếng, điểm tâm trạng trung bình của bạn là " + num(f.get("shortSleepMood"))
                      + ", so với " + num(f.get("longerSleepMood")) + " khi ngủ nhiều hơn. Đây là mối liên hệ quan sát được, không phải nguyên nhân."
                    : "Trong " + finding.sampleSize() + " ngày gần đây, thời lượng ngủ và điểm tâm trạng của bạn có xu hướng đi "
                      + (up ? "cùng" : "ngược") + " chiều. Đây là mối liên hệ quan sát được, không phải nguyên nhân.")
                    + " Điều này có đúng với cảm nhận của bạn không?";
            case ContrastDetectors.EXCEPTION -> exception(f, tagLabel);
            case ContrastDetectors.CONDITION_SHIFT -> capitalize(condition(f, tagLabel)) + ", tâm trạng của bạn nay là "
                    + num(f.get("nowWith")) + " so với " + num(f.get("nowWithout")) + " ở những ngày khác; 30 ngày trước là "
                    + num(f.get("prevWith")) + " so với " + num(f.get("prevWithout")) + ". "
                    + (up ? "Khoảng cách đã thu hẹp lại. Bạn đã làm gì khác đi trong những ngày ấy?"
                          : "Khoảng cách đang rộng ra. Dạo này những ngày ấy có điều gì nặng hơn không?"
                            + " Thử ghi lại một điều nhỏ có thể giúp bạn dễ thở hơn vào lần tới nhé.");
            case ContrastDetectors.TOPIC_SHIFT -> "So với 30 ngày trước, bạn viết về " + topic(f.get("topic"))
                    + (up ? " nhiều" : " ít") + " hơn: " + f.get("nowCount") + " trên " + f.get("nowTotal")
                    + " bài, trước đó là " + f.get("prevCount") + " trên " + f.get("prevTotal") + " bài. "
                    + (up ? "Có điều gì mới đang diễn ra ở phần này của cuộc sống bạn không?"
                          : "Phần này của cuộc sống đang yên hơn, hay bạn đang ít có dịp nghĩ tới nó?");
            case ContrastDetectors.RECOVERY_SHIFT -> up
                    ? "Sau những ngày tâm trạng xuống thấp, dạo này bạn thường lấy lại nhịp sau khoảng "
                      + num(f.get("nowDays")) + " ngày, so với " + num(f.get("prevDays")) + " ngày ở 30 ngày trước."
                      + " Bạn đã làm gì khác đi trong những lần ấy? Ghi lại để lần sau có thể dùng lại nhé."
                    : "Sau những ngày tâm trạng xuống thấp, dạo này bạn cần khoảng " + num(f.get("nowDays"))
                      + " ngày để lấy lại nhịp, lâu hơn " + num(f.get("prevDays")) + " ngày ở 30 ngày trước."
                      + " Có thể bạn đang mang nhiều thứ hơn. Tuần này thử dành cho mình một khoảng nghỉ nhỏ nhé?";
            default -> throw new IllegalArgumentException("Unknown insight type");
        };
    }

    private static String exception(Map<String, Object> f, String tagLabel) {
        String opening = capitalize(condition(f, tagLabel)) + ", tâm trạng của bạn thường thấp hơn: "
                + num(f.get("withAverage")) + " so với " + num(f.get("withoutAverage")) + " ở những ngày khác. Nhưng có "
                + f.get("exceptionDays") + " ngày như vậy bạn vẫn thấy ổn";
        if (!f.containsKey("featureKind"))
            return opening + ". Điều gì trong những ngày ấy đã khác đi? Lần tới gặp một ngày như vậy, thử ghi lại"
                    + " một điều giúp bạn đứng vững nhé.";
        return opening + ", và " + f.get("featureDays") + " ngày trong số đó " + feature(f, tagLabel) + ". "
                + reflection(String.valueOf(f.get("featureKind")), String.valueOf(f.get("featureCode")));
    }

    /** "những ngày có nhiều cuộc họp", "những ngày bạn viết về công việc", ... */
    private static String condition(Map<String, Object> f, String tagLabel) {
        Object code = f.get("conditionCode");
        return switch (String.valueOf(f.get("conditionKind"))) {
            case ContrastDetectors.ACTIVITY -> "những ngày có " + activity(code);
            case ContrastDetectors.TOPIC -> "những ngày bạn viết về " + topic(code);
            case ContrastDetectors.TAG -> "những ngày có bài gắn thẻ \"" + tagLabel + "\"";
            default -> "những ngày ngủ dưới 6 tiếng";
        };
    }

    private static String feature(Map<String, Object> f, String tagLabel) {
        Object code = f.get("featureCode");
        return switch (String.valueOf(f.get("featureKind"))) {
            case ContrastDetectors.ACTIVITY -> "có " + activity(code);
            case ContrastDetectors.TOPIC -> "bạn viết về " + topic(code);
            case ContrastDetectors.TAG -> "có bài gắn thẻ \"" + tagLabel + "\"";
            default -> "bạn ngủ từ 7 tiếng trở lên";
        };
    }

    /**
     * A possibility drawn from well-studied basic needs (connection, a body that moves and rests, time that
     * is your own), phrased as a question for the user to check, never as a trait.
     */
    private static String reflection(String kind, String code) {
        String need = switch (code) {
            case "social", "family-time", "eating-out", "RELATIONSHIPS" -> "sự kết nối với người khác";
            case "walking", "running", "stretching", "exercise", "nap", "HEALTH", "SLEEP" -> "việc cơ thể được vận động và nghỉ ngơi";
            case "reading", "studying", "cooking", "housework", "meditation", "journaling", "SELF_CARE" ->
                    "những khoảng thời gian bạn tự chọn cho mình";
            default -> ContrastDetectors.LONG_SLEEP.equals(kind) ? "việc cơ thể được nghỉ ngơi đủ" : null;
        };
        if (need == null) return "Điều gì trong những ngày ấy giúp bạn giữ được nhịp? Lần tới, thử ghi lại một điều như vậy nhé.";
        return "Có thể " + need + " là một điểm tựa của bạn. Điều đó có đúng với bạn không?"
                + " Tuần này thử để ý xem nó có lặp lại không nhé.";
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
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
                case "conditionCode", "featureCode" -> {
                    String kind = String.valueOf(finding.facts().get(key.replace("Code", "Kind")));
                    out.put(key, ContrastDetectors.ACTIVITY.equals(kind) ? activity(value) : topic(value));
                }
                default -> out.put(key, value instanceof BigDecimal d ? num(d) : value);
            }
        });
        out.put("sampleSize", finding.sampleSize());
        return out;
    }

    public static String num(Object value) {
        if (value == null) return "";
        BigDecimal d = value instanceof BigDecimal b ? b : new BigDecimal(value.toString());
        return d.stripTrailingZeros().scale() <= 0 ? d.toBigInteger().toString() : d.toPlainString().replace('.', ',');
    }

    public static String metric(Object code) {
        return "STRESS".equals(code) ? "căng thẳng" : "tâm trạng";
    }

    public static String weekday(Object code) {
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

    public static String emotion(Object code) {
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

    public static String topic(Object code) {
        return switch (String.valueOf(code)) {
            case "WORK" -> "công việc";
            case "RELATIONSHIPS" -> "các mối quan hệ";
            case "HEALTH" -> "sức khỏe";
            case "SLEEP" -> "giấc ngủ";
            case "SELF_CARE" -> "chăm sóc bản thân";
            default -> "chủ đề khác";
        };
    }

    public static String activity(Object code) {
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
