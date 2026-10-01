package com.mylog.knowledge.application;

import com.mylog.analysis.application.KnowledgeRetriever;
import com.mylog.user.application.UserProfileUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class RecommendationService {
    private final KnowledgeRetriever retriever;
    private final UserProfileUseCase profiles;
    public RecommendationService(KnowledgeRetriever retriever, UserProfileUseCase profiles) {
        this.retriever=retriever; this.profiles=profiles;
    }
    public List<KnowledgeRetriever.Passage> passages(UUID userId, String topicCode) {
        String locale=profiles.get(userId).locale();
        return retriever.retrieve(topicCode,locale,3);
    }
}
