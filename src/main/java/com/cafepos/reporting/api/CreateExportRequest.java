package com.cafepos.reporting.api;

import com.cafepos.reporting.domain.ExportType;
import jakarta.validation.constraints.NotNull;

public record CreateExportRequest(
        @NotNull(message = "Export type is required")
        ExportType exportType,

        String format
) {
}
