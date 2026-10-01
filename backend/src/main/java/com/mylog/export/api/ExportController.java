package com.mylog.export.api;

import com.mylog.export.api.request.AuthorizeDownloadRequest;
import com.mylog.export.api.request.CreateExportRequest;
import com.mylog.export.api.response.DownloadGrantResponse;
import com.mylog.export.api.response.ExportResponse;
import com.mylog.export.application.ExportService;
import com.mylog.platform.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exports")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class ExportController {
    private final ExportService exports;
    private final CurrentUserProvider current;
    public ExportController(ExportService exports, CurrentUserProvider current) {
        this.exports=exports;this.current=current;
    }
    @PostMapping public ResponseEntity<ExportResponse> create(@Valid @RequestBody CreateExportRequest request) {
        var view=exports.request(user(),request.format());
        return ResponseEntity.accepted().body(ExportResponse.from(view));
    }
    @GetMapping("/{id}") public ExportResponse get(@PathVariable UUID id) {
        return ExportResponse.from(exports.get(user(),id));
    }
    @PostMapping("/{id}:authorize-download")
    public DownloadGrantResponse authorize(@PathVariable UUID id, @Valid @RequestBody AuthorizeDownloadRequest request) {
        return DownloadGrantResponse.from(exports.authorizeDownload(user(),id,request.password()));
    }
    @GetMapping("/{id}/file") public ResponseEntity<byte[]> download(@PathVariable UUID id,
            @RequestParam long expires,@RequestParam String signature) {
        var file=exports.download(user(),id,expires,signature);
        String extension=file.format().toLowerCase(java.util.Locale.ROOT);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"mylog-export."+extension+"\"")
                .header("X-Content-Type-Options","nosniff")
                .contentType("PDF".equals(file.format())?MediaType.APPLICATION_PDF:MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(file.bytes());
    }
    private UUID user() { return current.requireCurrent().userId(); }
}
