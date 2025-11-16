package com.energyoptimiser.cafe.service;

import com.energyoptimiser.cafe.dto.InsightsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OptimizationServiceTest {

    private OptimizationService optimizationService;

    @BeforeEach
    void setUp() {
        optimizationService = new OptimizationService();
    }

    private AnalyticsService.AnalyticsData analytics(Map<Integer, Double> hourly,
                                                     Map<LocalDate, Double> daily,
                                                     List<Integer> peaks) {
        double avg = daily.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double max = daily.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        return new AnalyticsService.AnalyticsData(1L, hourly, daily, peaks, avg, max);
    }

    @Test
    void generateRecommendations_handlesNoData() {
        var recs = optimizationService.generateRecommendations(
                analytics(Map.of(), Map.of(), List.of())
        );
        assertThat(recs).isNotEmpty();
        assertThat(recs.getFirst()).contains("No data available");
    }

    @Test
    void generateRecommendations_includesPeakShiftingAndHourHints() {
        var hourly = Map.of(8, 2.0, 9, 3.0, 17, 5.0);
        var daily = Map.of(LocalDate.of(2025,1,1), 10.0);
        var peaks = List.of(17, 9);

        var recs = optimizationService.generateRecommendations(analytics(hourly, daily, peaks));

        assertThat(recs.stream().anyMatch(s -> s.contains("peak hour"))).isTrue();
        assertThat(recs.stream().anyMatch(s -> s.contains("Hour 17"))).isTrue();
        assertThat(recs.stream().anyMatch(s -> s.contains("Hour 9"))).isTrue();
    }

    @Test
    void generateRecommendations_variabilityAndBaseline() {
        // average 10, max 13 (> 1.25 * 10 == 12.5) -> triggers variability
        var hourly = Map.of(0, 1.0, 1, 1.0); // baseline > 0 triggers baseline advice
        var daily = Map.of(LocalDate.of(2025,1,1), 7.0,
                           LocalDate.of(2025,1,2), 13.0);
        var peaks = List.of(1);

        var recs = optimizationService.generateRecommendations(analytics(hourly, daily, peaks));

        assertThat(recs.stream().anyMatch(s -> s.toLowerCase().contains("variability"))).isTrue();
        assertThat(recs.stream().anyMatch(s -> s.toLowerCase().contains("baseline"))).isTrue();
    }

    @Test
    void generateInsights_buildsDtoWithAnalyticsAndRecommendations() {
        var hourly = Map.of(8, 2.0, 9, 3.0, 17, 5.0);
        var daily = Map.of(LocalDate.of(2025,1,1), 10.0,
                           LocalDate.of(2025,1,2), 12.0);
        var peaks = List.of(17, 9);

        var analytics = analytics(hourly, daily, peaks);

        InsightsResponse resp = optimizationService.generateInsights(analytics);

        assertThat(resp.hourlyUsage()).isEqualTo(hourly);
        assertThat(resp.dailyUsage()).isEqualTo(daily);
        assertThat(resp.peakHours()).isEqualTo(peaks);
        assertThat(resp.averageDailyUsage()).isEqualTo(11.0);
        assertThat(resp.maxDailyUsage()).isEqualTo(12.0);
        assertThat(resp.recommendations()).isNotEmpty();
    }
}
