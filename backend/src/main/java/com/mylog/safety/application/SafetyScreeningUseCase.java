package com.mylog.safety.application;

public interface SafetyScreeningUseCase {
    SafetyDecision screen(String plainText);

    default SafetyDecision screen(String plainText, boolean externalConsentGranted) {
        return screen(plainText);
    }
}
