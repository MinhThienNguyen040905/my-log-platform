package com.mylog.feedback.application;

import com.mylog.feedback.application.query.FeedbackView;
import com.mylog.platform.crypto.SensitiveDataCipher;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedbackStore {
    void create(UUID id,UUID userId,String category,SensitiveDataCipher.Encrypted message,Instant now);
    Optional<FeedbackView> find(UUID id,UUID userId,boolean admin);
    List<FeedbackView> list(String status,int limit);
    List<FeedbackView> listOwn(UUID userId);
    void update(UUID id,String status,UUID assignee,long expectedVersion,Instant now);
    boolean canAssign(UUID userId);
    void audit(UUID actor,UUID target,String action,Instant now);
    void expire(Instant now);
}
