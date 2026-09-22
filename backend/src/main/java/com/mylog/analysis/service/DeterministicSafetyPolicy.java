package com.mylog.analysis.service;

import com.mylog.analysis.entity.SafetyDecision;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DeterministicSafetyPolicy {

    private static final Set<String> CRITICAL_MARKERS = Set.of(
            "kill myself", "suicide", "end my life", "want to die",
            "tu tu", "ket lieu doi minh", "muon chet");
    private static final Set<String> HIGH_MARKERS = Set.of(
            "hurt myself", "self harm", "harm someone", "khong muon song",
            "lam hai ban than", "hai nguoi khac");

    public SafetyDecision evaluate(String content) {
        String normalized = Normalizer.normalize(content == null ? "" : content, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        if (CRITICAL_MARKERS.stream().anyMatch(normalized::contains)) {
            return new SafetyDecision("CRITICAL", true, "SHOW_CRISIS_SUPPORT");
        }
        if (HIGH_MARKERS.stream().anyMatch(normalized::contains)) {
            return new SafetyDecision("HIGH", true, "SHOW_URGENT_SUPPORT");
        }
        return new SafetyDecision("NORMAL", false, "NONE");
    }
}
