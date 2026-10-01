package com.mylog.export.application;

import com.mylog.checkin.application.CheckinService;
import com.mylog.identity.application.IdentityService;
import com.mylog.journal.application.JournalService;
import com.mylog.journal.application.JournalTagService;
import com.mylog.analysis.application.AnalysisQueries;
import com.mylog.insight.application.InsightService;
import com.mylog.reporting.application.ReportService;
import com.mylog.feedback.application.FeedbackService;
import com.mylog.selfcare.application.SelfCareService;
import com.mylog.user.application.UserProfileUseCase;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class ExportSnapshot {
    private final IdentityService identity;
    private final UserProfileUseCase profiles;
    private final JournalService journals;
    private final JournalTagService tags;
    private final AnalysisQueries analyses;
    private final InsightService insights;
    private final ReportService reports;
    private final FeedbackService feedback;
    private final CheckinService checkins;
    private final SelfCareService selfcare;
    ExportSnapshot(IdentityService identity, UserProfileUseCase profiles, JournalService journals,
                   JournalTagService tags,AnalysisQueries analyses,InsightService insights,ReportService reports,
                   FeedbackService feedback,
                   CheckinService checkins, SelfCareService selfcare) {
        this.identity=identity; this.profiles=profiles; this.journals=journals;
        this.tags=tags;this.analyses=analyses;this.insights=insights;this.reports=reports;
        this.feedback=feedback;
        this.checkins=checkins; this.selfcare=selfcare;
    }
    record Row(String section, String id, Object value) {}
    List<Row> capture(UUID userId) {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("account", userId.toString(), java.util.Map.of("email",identity.exportEmail(userId))));
        rows.add(new Row("profile", userId.toString(), profiles.get(userId)));
        profiles.consents(userId).forEach(c -> rows.add(new Row("consent",c.type(),c)));
        identity.exportSessions(userId).forEach(s -> rows.add(new Row("session",s.id().toString(),s)));
        journals.exportAll(userId).forEach(j -> {
                rows.add(new Row("journal",j.id().toString(),j));
                if ("ANALYZED".equals(j.analysisStatus()))
                    rows.add(new Row("analysis",j.id().toString(),analyses.exportRetained(userId,j.id())));
        });
        tags.list(userId).forEach(t -> rows.add(new Row("journal_tag",t.id().toString(),t)));
        tags.exportLinks(userId).forEach(link -> rows.add(new Row("journal_tag_link",
                link.journalEntryId()+":"+link.tagId(),link)));
        checkins.exportAll(userId).forEach(c -> rows.add(new Row("checkin",c.id().toString(),c)));
        selfcare.goals(userId).forEach(g -> rows.add(new Row("goal",g.id().toString(),g)));
        selfcare.exportCompletions(userId).forEach(c -> rows.add(new Row("habit_completion",c.id().toString(),c)));
        feedback.exportOwn(userId).forEach(f -> rows.add(new Row("feedback",f.id().toString(),f)));
        String cursor=null;
        do {
            var page=insights.list(userId,null,null,cursor,50);
            page.items().forEach(i -> rows.add(new Row("insight",i.id().toString(),i)));
            cursor=page.nextCursor();
        } while (cursor!=null);
        cursor=null;
        do {
            var page=reports.list(userId,null,cursor,50);
            page.items().forEach(r -> rows.add(new Row("report",r.id().toString(),reports.get(userId,r.id()))));
            cursor=page.nextCursor();
        } while (cursor!=null);
        return rows;
    }
}
