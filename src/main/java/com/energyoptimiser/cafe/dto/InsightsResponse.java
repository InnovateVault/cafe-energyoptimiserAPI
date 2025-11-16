package com.energyoptimiser.cafe.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


/**
 * DTO for insight response outcomes.
 * Provides a stable, read‑only view of the processed file.
 */
public record InsightsResponse(
        Map<Integer, Double> hourlyUsage,
        Map<LocalDate, Double> dailyUsage,
        List<Integer> peakHours,
        double averageDailyUsage,
        double maxDailyUsage,
        List<String> recommendations
) {
}
