package com.mylog.knowledge.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReflectiveQuestionsRequest(
        @NotBlank @Pattern(regexp = "[A-Z0-9_]{1,48}") String topicCode) {}
