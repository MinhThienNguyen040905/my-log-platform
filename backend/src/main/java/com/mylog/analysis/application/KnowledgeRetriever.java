package com.mylog.analysis.application;

import java.util.List;

public interface KnowledgeRetriever {
    record Passage(String id, String text, String sourceVersion) {}
    List<Passage> retrieve(String topicCode, int limit);
}
