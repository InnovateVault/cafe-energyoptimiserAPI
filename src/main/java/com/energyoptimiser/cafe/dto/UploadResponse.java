package com.energyoptimiser.cafe.dto;

import java.time.LocalDateTime;


/**
 * DTO for CSV upload outcomes.
 * Provides a stable, read‑only view of the processed file.
 */
public record UploadResponse(
        Long cafeId,
        String fileName,
        int rowsImported,
        String status,
        LocalDateTime processedAt
) {
}
