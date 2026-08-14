package com.cafepos.reporting.api;

import com.cafepos.reporting.application.ReportingApplicationService;
import com.cafepos.reporting.domain.ExportJob;
import com.cafepos.shared.tenancy.TenantContextHolder;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exports")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MANAGER')")
public class ExportJobController {

    private final ReportingApplicationService reportingApplicationService;

    public ExportJobController(ReportingApplicationService reportingApplicationService) {
        this.reportingApplicationService = Objects.requireNonNull(reportingApplicationService, "reportingApplicationService must not be null");
    }

    @PostMapping
    public ResponseEntity<ExportJobResponse> triggerExport(@Valid @RequestBody CreateExportRequest request) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        String format = (request.format() != null && !request.format().isBlank()) ? request.format() : "CSV";
        ExportJob job = reportingApplicationService.triggerExport(cafeId, request.exportType(), format);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(toResponse(job));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExportJobResponse> getExportStatus(@PathVariable("id") UUID id) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        ExportJob job = reportingApplicationService.getExportJob(cafeId, id);
        return ResponseEntity.ok(toResponse(job));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadExportFile(@PathVariable("id") UUID id) {
        UUID cafeId = UUID.fromString(TenantContextHolder.getRequiredTenantId());
        ExportJob job = reportingApplicationService.getExportJob(cafeId, id);
        if (job.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(job.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file);
        MediaType mediaType = "XLSX".equalsIgnoreCase(job.getFormat())
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.TEXT_PLAIN;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }

    private ExportJobResponse toResponse(ExportJob job) {
        String downloadUrl = job.getFilePath() != null ? "/api/v1/exports/" + job.getId() + "/download" : null;
        return new ExportJobResponse(
                job.getId(),
                job.getCafeId(),
                job.getExportType(),
                job.getFormat(),
                job.getStatus(),
                downloadUrl,
                job.getCreatedAt(),
                job.getCompletedAt()
        );
    }
}
