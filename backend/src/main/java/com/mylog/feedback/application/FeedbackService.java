package com.mylog.feedback.application;

import com.mylog.feedback.application.query.FeedbackView;
import com.mylog.platform.crypto.SensitiveDataCipher;
import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.platform.web.ResourceNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class FeedbackService {
    private final FeedbackStore store;
    private final SensitiveDataCipher cipher;
    private final IdGenerator ids;
    private final Clock clock;
    @Value("${mylog.app-profile:all}") private String appProfile;
    public FeedbackService(FeedbackStore store,SensitiveDataCipher cipher,IdGenerator ids,Clock clock) {
        this.store=store;this.cipher=cipher;this.ids=ids;this.clock=clock;
    }
    @Transactional
    public UUID submit(UUID userId,String category,String message) {
        if (!Set.of("BUG","IDEA","OTHER").contains(category) || message==null
                || message.isBlank() || message.length()>4000) throw new InvalidRequestException();
        UUID id=ids.next();
        store.create(id,userId,category,cipher.encrypt("feedback.message",userId,id,message.trim()),clock.instant());
        return id;
    }
    @Transactional(readOnly = true)
    public FeedbackView own(UUID userId,UUID id) {
        return store.find(id,userId,false).orElseThrow(() -> new ResourceNotFoundException("Feedback not found."));
    }
    @Transactional(readOnly = true)
    public List<FeedbackView> exportOwn(UUID userId) {return store.listOwn(userId);}
    @Transactional
    public FeedbackView adminGet(UUID actor,UUID id) {
        FeedbackView view=store.find(id,actor,true).orElseThrow(() -> new ResourceNotFoundException("Feedback not found."));
        store.audit(actor,id,"FEEDBACK_READ",clock.instant());
        return view;
    }
    @Transactional
    public List<FeedbackView> adminList(UUID actor,String status,int limit) {
        if (status!=null && !Set.of("OPEN","IN_PROGRESS","RESOLVED","CLOSED").contains(status)
                || limit<1 || limit>50) throw new InvalidRequestException();
        var views=store.list(status,limit);
        for (var view:views) store.audit(actor,view.id(),"FEEDBACK_READ",clock.instant());
        return views;
    }
    @Transactional
    public FeedbackView adminUpdate(UUID actor,UUID id,String status,UUID assignee,long expectedVersion) {
        if (!Set.of("OPEN","IN_PROGRESS","RESOLVED","CLOSED").contains(status)) throw new InvalidRequestException();
        if (assignee!=null && !store.canAssign(assignee)) throw new InvalidRequestException();
        store.update(id,status,assignee,expectedVersion,clock.instant());
        store.audit(actor,id,"FEEDBACK_UPDATED",clock.instant());
        return store.find(id,actor,true).orElseThrow();
    }
    @Scheduled(fixedDelayString="${mylog.feedback.retention-delay-ms:86400000}")
    @Transactional public void cleanup() {if (!"api".equals(appProfile)) store.expire(clock.instant());}
}
