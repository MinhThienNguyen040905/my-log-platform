package com.mylog.knowledge.api.response;

import java.util.List;

public record ReflectiveQuestionsResponse(String topicCode, List<String> questions, String message) {}
