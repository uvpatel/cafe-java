package com.cafepos.reporting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "export_jobs")
public class ExportJob {

    @Id
    private UUID id;

    @Column(name = "cafe_id", nullable = false)
    private UUID cafeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false, length = 30)
    private ExportType exportType;

    @Column(name = "format", nullable = false, length = 10)
    private String format;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExportStatus status;

    @Column(name = "file_path", length = 255)
    private String filePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ExportJob() {
        // JPA standard constructor
    }

    public ExportJob(UUID id, UUID cafeId, ExportType exportType, String format) {
        validateInputs(cafeId, exportType, format);
        this.id = id != null ? id : UUID.randomUUID();
        this.cafeId = cafeId;
        this.exportType = exportType;
        this.format = format.trim().toUpperCase();
        this.status = ExportStatus.PENDING;
    }

    public ExportJob(UUID cafeId, ExportType exportType, String format) {
        this(UUID.randomUUID(), cafeId, exportType, format);
    }

    private static void validateInputs(UUID cafeId, ExportType exportType, String format) {
        if (cafeId == null) {
            throw new IllegalArgumentException("Cafe ID must not be null");
        }
        if (exportType == null) {
            throw new IllegalArgumentException("Export type must not be null");
        }
        if (format == null || format.isBlank()) {
            throw new IllegalArgumentException("Format must not be empty");
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    public void markProcessing() {
        if (this.status != ExportStatus.PENDING) {
            throw new IllegalStateException("Export job can only transition to PROCESSING from PENDING");
        }
        this.status = ExportStatus.PROCESSING;
    }

    public void markCompleted(String filePath) {
        if (this.status != ExportStatus.PROCESSING && this.status != ExportStatus.PENDING) {
            throw new IllegalStateException("Export job can only complete from PENDING or PROCESSING");
        }
        this.filePath = filePath;
        this.status = ExportStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markFailed() {
        this.status = ExportStatus.FAILED;
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCafeId() {
        return cafeId;
    }

    public ExportType getExportType() {
        return exportType;
    }

    public String getFormat() {
        return format;
    }

    public ExportStatus getStatus() {
        return status;
    }

    public String getFilePath() {
        return filePath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExportJob exportJob = (ExportJob) o;
        return Objects.equals(id, exportJob.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
