package com.mylog.knowledge.api;

import com.mylog.knowledge.api.request.*;
import com.mylog.knowledge.api.response.KnowledgeVersionResponse;
import com.mylog.knowledge.application.KnowledgeService;
import com.mylog.platform.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/knowledge-items")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class KnowledgeAdminController {
    private final KnowledgeService service;
    private final CurrentUserProvider current;
    public KnowledgeAdminController(KnowledgeService service, CurrentUserProvider current) {
        this.service=service; this.current=current;
    }
    @PostMapping
    @PreAuthorize("hasAuthority('knowledge:write')")
    public ResponseEntity<KnowledgeVersionResponse> create(@Valid @RequestBody CreateKnowledgeItemRequest r) {
        var v=service.create(user(),r.slug(),r.topicCode(),r.locale(),r.sourceName(),r.sourceUrl(),
                r.ownerTeam(),r.title(),r.content(),r.effectiveFrom(),r.effectiveTo());
        return ResponseEntity.created(URI.create("/api/v1/admin/knowledge-items/"+v.itemId()+"/versions/1"))
                .body(KnowledgeVersionResponse.from(v));
    }
    @GetMapping("/{itemId}/versions/{version}")
    @PreAuthorize("hasAnyAuthority('knowledge:write','knowledge:review')")
    public KnowledgeVersionResponse get(@PathVariable UUID itemId, @PathVariable int version) {
        return KnowledgeVersionResponse.from(service.get(itemId,version));
    }
    @PostMapping("/{itemId}/versions")
    @PreAuthorize("hasAuthority('knowledge:write')")
    public KnowledgeVersionResponse revise(@PathVariable UUID itemId, @Valid @RequestBody WriteKnowledgeVersionRequest r) {
        return KnowledgeVersionResponse.from(service.revise(user(),itemId,r.title(),r.content(),
                r.effectiveFrom(),r.effectiveTo()));
    }
    @PatchMapping("/{itemId}/versions/{version}")
    @PreAuthorize("hasAuthority('knowledge:write')")
    public KnowledgeVersionResponse update(@PathVariable UUID itemId,@PathVariable int version,
                                           @Valid @RequestBody WriteKnowledgeVersionRequest r) {
        return KnowledgeVersionResponse.from(service.updateDraft(user(),itemId,version,r.title(),r.content()));
    }
    @PostMapping("/{itemId}/versions/{version}:submit")
    @PreAuthorize("hasAuthority('knowledge:write')")
    public KnowledgeVersionResponse submit(@PathVariable UUID itemId,@PathVariable int version) {
        return KnowledgeVersionResponse.from(service.submit(user(),itemId,version));
    }
    @PostMapping("/{itemId}/versions/{version}:approve")
    @PreAuthorize("hasAuthority('knowledge:review')")
    public KnowledgeVersionResponse approve(@PathVariable UUID itemId,@PathVariable int version) {
        return KnowledgeVersionResponse.from(service.review(user(),itemId,version,true,null));
    }
    @PostMapping("/{itemId}/versions/{version}:reject")
    @PreAuthorize("hasAuthority('knowledge:review')")
    public KnowledgeVersionResponse reject(@PathVariable UUID itemId,@PathVariable int version,
                                           @Valid @RequestBody KnowledgeReasonRequest r) {
        return KnowledgeVersionResponse.from(service.review(user(),itemId,version,false,r.reasonCode()));
    }
    @PostMapping("/{itemId}/versions/{version}:archive")
    @PreAuthorize("hasAuthority('knowledge:review')")
    public KnowledgeVersionResponse archive(@PathVariable UUID itemId,@PathVariable int version,
                                            @Valid @RequestBody KnowledgeReasonRequest r) {
        return KnowledgeVersionResponse.from(service.archive(user(),itemId,version,r.reasonCode()));
    }
    private UUID user() { return current.requireCurrent().userId(); }
}
