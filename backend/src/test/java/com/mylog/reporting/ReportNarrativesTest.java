package com.mylog.reporting;

import com.mylog.reporting.application.ReportNarratives;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReportNarrativesTest {
    @Test void weeklySummaryComparesWithPreviousWeekUsingVietnameseDecimals() {
        Map<String, Object> s = snapshot("6.87", "6.43", "4.90", "4.70");
        s.put("bestDay", Map.of("date", "2026-11-07", "moodScore", new BigDecimal("8.6")));
        s.put("topEmotions", List.of(Map.of("code", "CALM", "count", 5)));
        String text = ReportNarratives.render("WEEKLY", s);
        assertTrue(text.startsWith("Tuần này bạn viết nhật ký 6/7 ngày, chuỗi dài nhất 4 ngày liền."), text);
        assertTrue(text.contains("Điểm tâm trạng trung bình là 6,9/10, nhích lên so với tuần trước (6,4)."), text);
        assertTrue(text.contains("Ngày dễ chịu nhất là thứ Bảy 7/11 (8,6)."), text);
        assertTrue(text.contains("Sắc thái xuất hiện nhiều nhất trong các bài viết là bình yên."), text);
        assertFalse(text.contains("6.87") || text.contains("Mức căng thẳng"), text);
        assertFalse(ReportNarratives.isHarder(s));
    }

    @Test void harderMonthGetsGentleClosingAndNoCauseLanguage() {
        Map<String, Object> s = snapshot("4.20", "6.00", "7.10", "5.00");
        String text = ReportNarratives.render("MONTHLY", s);
        assertTrue(text.startsWith("Tháng này"), text);
        assertTrue(text.contains("thấp hơn tháng trước (6)"), text);
        assertTrue(text.contains("Mức căng thẳng tăng từ 5 lên 7,1."), text);
        assertTrue(text.endsWith("Tháng tới, hãy dành cho mình vài khoảng nghỉ nhỏ nhé."), text);
        assertTrue(ReportNarratives.isHarder(s));
        assertFalse(text.contains(" vì ") || text.contains("nguyên nhân"));
    }

    @Test void emptyPeriodStillReadsNaturally() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("journalDays", 0); s.put("periodDays", 7);
        assertEquals("Tuần này bạn chưa viết nhật ký ngày nào. Hãy giữ lại những điều đã giúp tuần này nhẹ nhàng nhé.",
                ReportNarratives.render("WEEKLY", s));
    }

    private static Map<String, Object> snapshot(String mood, String previousMood, String stress, String previousStress) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("journalDays", 6L); s.put("periodDays", 7L); s.put("longestJournalStreak", 4);
        s.put("averageMood", new BigDecimal(mood)); s.put("averageStress", new BigDecimal(stress));
        s.put("previous", Map.of("averageMood", new BigDecimal(previousMood), "averageStress", new BigDecimal(previousStress)));
        return s;
    }
}
