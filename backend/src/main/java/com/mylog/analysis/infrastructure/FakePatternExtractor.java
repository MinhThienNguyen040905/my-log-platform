package com.mylog.analysis.infrastructure;

import com.mylog.analysis.application.PatternExtractor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Keyword rules for local development and tests. Never calls a network and is not a quality baseline: it
 * exists so storage, the 30-day rules, the API and the UI can be exercised without a provider.
 */
final class FakePatternExtractor implements PatternExtractor {
    static final String PROVIDER = "fake";
    static final String MODEL = "keyword-rules";
    static final String VERSION = "fake-tp-v1";
    private static final BigDecimal CONFIDENCE = new BigDecimal("0.7500");
    private static final Pattern SENTENCES = Pattern.compile("(?<=[.!?…])\\s+|\\n+");

    private static final Map<String, Pattern> RULES = new LinkedHashMap<>();
    static {
        rule("CATASTROPHIZING", "coi như (hết|xong|bỏ)|sụp đổ|tiêu tan|đổ bể|chắc chắn sẽ (mất|hỏng|trượt|rớt|bị)");
        rule("ALL_OR_NOTHING", "thất bại hoàn toàn|công cốc|vứt đi|hỏng bét|đổ sông đổ biển|hoặc là .+ hoặc là");
        rule("OVERGENERALIZATION", "lúc nào cũng vậy|chẳng bao giờ|không ai .*bao giờ|lần nào (cũng|chả)|mãi mãi ở|luôn là người bị");
        rule("MIND_READING", "chắc (họ|mọi người|ai cũng|cả nhóm|sếp|thầy|mẹ|bố|anh ấy|chị ấy|bạn ấy|tụi nó)"
                + ".{0,30}(nghĩ|thấy|giận|thất vọng|chán|khó chịu|không hài lòng)|nhìn mặt .{0,30} là biết");
        rule("SHOULD_STATEMENTS", "đáng lẽ|lẽ ra|phải luôn|không được (khóc|thấy|để ai)|mới xứng đáng");
        rule("SELF_LABELING", "đúng là (đồ|một đứa|kẻ|đứa)|mình là (kẻ|đồ|đứa|loại người)|ngu thật");
        rule("PERSONALIZATION", "tại mình|là do mình|lỗi của mình");
        rule("DISCOUNTING_POSITIVE", "chỉ là may|nói cho có|xã giao|ai chả làm được|chuyện nhỏ thôi|chẳng có gì đáng kể");
        rule("EMOTIONAL_REASONING", "(thấy|cảm thấy|cảm giác).{0,40}(nên|vậy|nghĩa là) chắc");
        rule("RUMINATION", "nghĩ đi nghĩ lại|tua lại|không dứt ra|cứ nghĩ mãi|lặp (lại )?trong đầu|giá như");
        rule("WORRY_LOOP", "lo không ngủ|hết kịch bản|đủ kịch bản|không dừng được|lỡ .{1,30}, lỡ");
        rule("AVOIDANCE", "không dám (mở|xem|nói)|chưa dám mở|để máy im lặng|viện cớ|lần lữa");
        rule("PROCRASTINATION_GUILT", "ghét bản thân|tự trách|áy náy|chán bản thân|giận bản thân");
        rule("PERFECTIONISM", "lần thứ (ba|tư|năm|n)\\b|chưa đủ (tốt|đẹp)|chưa hoàn hảo|sửa đi sửa lại|trăm phần trăm");
        rule("PEOPLE_PLEASING", "không dám từ chối|ngại từ chối|sợ (bạn|họ|người ta) buồn|sợ mất lòng");
        rule("HARSH_SELF_CRITICISM", "thảm hại|không xứng đáng|tự mắng|chẳng ra gì|đáng xấu hổ|toàn khuyết điểm|tệ hại");
        rule("SOCIAL_WITHDRAWAL", "không muốn gặp ai|chẳng muốn nói chuyện|không bắt máy|từ chối hết");
    }

    private static void rule(String code, String regex) {
        RULES.put(code, Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    }

    @Override
    public Result extract(String title, String plainText) {
        List<Signal> signals = new ArrayList<>();
        for (String sentence : SENTENCES.split((title == null ? "" : title) + "\n" + (plainText == null ? "" : plainText))) {
            String trimmed = sentence.strip();
            if (trimmed.isEmpty()) continue;
            String lower = trimmed.toLowerCase(Locale.ROOT);
            RULES.forEach((code, pattern) -> {
                if (pattern.matcher(lower).find()) signals.add(new Signal(code, CONFIDENCE, trimmed));
            });
        }
        return new Result(true, signals, PROVIDER, MODEL, VERSION, 0, 0, BigDecimal.ZERO);
    }
}
