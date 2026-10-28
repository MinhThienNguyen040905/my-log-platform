package com.mylog.knowledge.application;

import com.mylog.analysis.application.KnowledgeRetriever;
import com.mylog.platform.web.DependencyUnavailableException;
import com.mylog.user.application.UserProfileUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class RecommendationService {
    private final KnowledgeRetriever retriever;
    private final ReflectiveQuestionGenerator questionGenerator;
    private final UserProfileUseCase profiles;
    public RecommendationService(KnowledgeRetriever retriever, ReflectiveQuestionGenerator questionGenerator,
                                 UserProfileUseCase profiles) {
        this.retriever=retriever; this.questionGenerator=questionGenerator; this.profiles=profiles;
    }
    public List<KnowledgeRetriever.Passage> passages(UUID userId, String topicCode) {
        String locale=profiles.get(userId).locale();
        if ("RECOMMEND_MODEL_DOCS".equals(topicCode) && "vi".equals(locale)) locale="vi-VN";
        return retriever.retrieve(topicCode,locale,3);
    }

    public List<String> reflectiveQuestions(UUID userId, String topicCode) {
        List<KnowledgeRetriever.Passage> excerpts = passages(userId, topicCode);
        if (excerpts.isEmpty()) return List.of();
        try {
            return questionGenerator.generate(topicCode, excerpts);
        } catch (RuntimeException exception) {
            throw new DependencyUnavailableException();
        }
    }
}
