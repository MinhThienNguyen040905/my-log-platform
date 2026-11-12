package com.mylog.reporting.application;

import com.mylog.insight.application.InsightTemplates;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Reviewed Vietnamese summary for a weekly/monthly report, built only from the computed snapshot. Describes
 * what happened in the period and how it compares with the previous one; never claims causes.
 */
public final class ReportNarratives {
    static final double MOOD_CHANGE = 0.3;
    static final double STRESS_CHANGE = 0.5;

    private ReportNarratives() {}

    public static String render(String type, Map<String, Object> s) {
        boolean monthly = "MONTHLY".equals(type);
        String period = monthly ? "Tháng" : "Tuần";
        String periodLower = monthly ? "tháng" : "tuần";
        StringBuilder out = new StringBuilder();

        int journalDays = intValue(s.get("journalDays"));
        int periodDays = intValue(s.get("periodDays"));
        int streak = intValue(s.get("longestJournalStreak"));
        if (journalDays == 0) out.append(period).append(" này bạn chưa viết nhật ký ngày nào.");
        else {
            out.append(period).append(" này bạn viết nhật ký ").append(journalDays).append('/').append(periodDays).append(" ngày");
            if (streak >= 3) out.append(", chuỗi dài nhất ").append(streak).append(" ngày liền");
            out.append('.');
        }

        BigDecimal mood = one(s.get("averageMood"));
        BigDecimal previousMood = one(previous(s, "averageMood"));
        if (mood != null) {
            out.append(" Điểm tâm trạng trung bình là ").append(num(mood)).append("/10");
            if (previousMood != null) {
                double diff = mood.subtract(previousMood).doubleValue();
                if (diff >= MOOD_CHANGE) out.append(", nhích lên so với ").append(periodLower).append(" trước (").append(num(previousMood)).append(')');
                else if (diff <= -MOOD_CHANGE) out.append(", thấp hơn ").append(periodLower).append(" trước (").append(num(previousMood)).append(')');
                else out.append(", gần như ").append(periodLower).append(" trước");
            }
            out.append('.');
        }

        BigDecimal stress = one(s.get("averageStress"));
        BigDecimal previousStress = one(previous(s, "averageStress"));
        if (stress != null && previousStress != null) {
            double diff = stress.subtract(previousStress).doubleValue();
            if (Math.abs(diff) >= STRESS_CHANGE) out.append(" Mức căng thẳng ").append(diff > 0 ? "tăng" : "giảm")
                    .append(" từ ").append(num(previousStress)).append(diff > 0 ? " lên " : " xuống ").append(num(stress)).append('.');
        }

        if (s.get("bestDay") instanceof Map<?, ?> best && best.get("date") != null) {
            LocalDate date = LocalDate.parse(best.get("date").toString());
            out.append(" Ngày dễ chịu nhất là ")
                    .append(monthly ? "ngày " : InsightTemplates.weekday(date.getDayOfWeek().name()) + " ")
                    .append(date.getDayOfMonth()).append('/').append(date.getMonthValue())
                    .append(" (").append(num(one(best.get("moodScore")))).append(").");
        }

        if (s.get("topEmotions") instanceof List<?> emotions && !emotions.isEmpty()
                && emotions.getFirst() instanceof Map<?, ?> top && top.get("code") != null) {
            out.append(" Sắc thái xuất hiện nhiều nhất trong các bài viết là ")
                    .append(InsightTemplates.emotion(top.get("code"))).append('.');
        }

        out.append(isHarder(s)
                ? " " + period + " tới, hãy dành cho mình vài khoảng nghỉ nhỏ nhé."
                : " Hãy giữ lại những điều đã giúp " + periodLower + " này nhẹ nhàng nhé.");
        return out.toString();
    }

    /** True when mood fell or stress rose noticeably versus the previous period. */
    public static boolean isHarder(Map<String, Object> s) {
        BigDecimal mood = one(s.get("averageMood")), previousMood = one(previous(s, "averageMood"));
        BigDecimal stress = one(s.get("averageStress")), previousStress = one(previous(s, "averageStress"));
        return mood != null && previousMood != null && mood.subtract(previousMood).doubleValue() <= -MOOD_CHANGE
                || stress != null && previousStress != null && stress.subtract(previousStress).doubleValue() >= STRESS_CHANGE;
    }

    static String num(BigDecimal value) {
        return value == null ? "" : InsightTemplates.num(value);
    }

    static BigDecimal one(Object value) {
        if (value == null) return null;
        BigDecimal d = value instanceof BigDecimal b ? b : new BigDecimal(value.toString());
        return d.setScale(1, RoundingMode.HALF_UP);
    }

    private static Object previous(Map<String, Object> s, String key) {
        return s.get("previous") instanceof Map<?, ?> p ? p.get(key) : null;
    }

    private static int intValue(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }
}
