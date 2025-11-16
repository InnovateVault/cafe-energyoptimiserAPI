package com.energyoptimiser.cafe.service;

import com.energyoptimiser.cafe.dto.InsightsResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides simple rule-based recommendations based on analytics data.
 */
@Service
public class OptimizationService {

    public List<String> generateRecommendations(AnalyticsService.AnalyticsData analytics) {
        List<String> recs = new ArrayList<>();

        if (analytics.hourlyUsage().isEmpty()) {
            recs.add("No data available to generate recommendations.");
            return recs;
        }

        // Peak-shifting suggestions
        if (!analytics.peakHours().isEmpty()) {
            recs.add("Consider shifting non-essential loads away from peak hour(s): " + analytics.peakHours());
        }

        // If max day significantly exceeds average, suggest demand smoothing
        if (analytics.maxDailyUsage() > analytics.averageDailyUsage() * 1.25) {
            recs.add("Daily usage variability is high. Explore staggering equipment startup and using timers.");
        }

        // Hour-specific hints
        for (Integer hour : analytics.peakHours()) {
            double kwh = analytics.hourlyUsage().getOrDefault(hour, 0.0);
            if (kwh > 0) {
                recs.add("Hour " + hour + ": schedule dishwasher/ice machine defrost outside this hour if possible.");
            }
        }

        // Baseline optimization
        double baseline = analytics.hourlyUsage().values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (baseline > 0) {
            recs.add("Review overnight standby loads; baseline hourly usage is ~" + String.format("%.2f", baseline) + " kWh.");
        }

        if (recs.isEmpty()) {
            recs.add("Usage appears balanced. Maintain current practices and monitor periodically.");
        }

        return recs;
    }

    /**
     * Convenience method to build an InsightsResponse DTO from analytics and generated recommendations.
     * This ensures the DTO is used at the service layer, even without a controller present.
     */
    public InsightsResponse generateInsights(AnalyticsService.AnalyticsData analytics) {
        List<String> recs = generateRecommendations(analytics);
        return new InsightsResponse(
                analytics.hourlyUsage(),
                analytics.dailyUsage(),
                analytics.peakHours(),
                analytics.averageDailyUsage(),
                analytics.maxDailyUsage(),
                recs
        );
    }
}
