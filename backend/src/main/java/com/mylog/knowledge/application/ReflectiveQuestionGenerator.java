package com.mylog.knowledge.application;

import com.mylog.analysis.application.KnowledgeRetriever;

import java.util.List;

public interface ReflectiveQuestionGenerator {
    List<String> generate(String topicCode, List<KnowledgeRetriever.Passage> excerpts);
}
