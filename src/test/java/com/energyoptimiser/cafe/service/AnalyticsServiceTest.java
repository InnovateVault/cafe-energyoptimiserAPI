package com.energyoptimiser.cafe.service;

import com.energyoptimiser.cafe.model.CafeProfile;
import com.energyoptimiser.cafe.model.EnergyReading;
import com.energyoptimiser.cafe.repository.EnergyReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * Unit tests for AnalyticsService using a mocked EnergyReadingRepository.
 */
class AnalyticsServiceTest {

    private EnergyReadingRepository energyReadingRepository;
    private AnalyticsService analyticsService;

    private CafeProfile cafe;

    @BeforeEach
    void setUp() {
        energyReadingRepository = Mockito.mock(EnergyReadingRepository.class);
        analyticsService = new AnalyticsService(energyReadingRepository);
        cafe = CafeProfile.builder().id(1L).name("Cafe A").location("Loc").build();
    }

    private List<EnergyReading> sampleReadingsForDaily() {
        // 2025-01-01: 10 + 5 kWh, 2025-01-02: 7 kWh
        return List.of(
                EnergyReading.builder().id(1L).cafe(cafe).timestamp(LocalDateTime.of(2025,1,1,8,0)).kwh(10).build(),
                EnergyReading.builder().id(2L).cafe(cafe).timestamp(LocalDateTime.of(2025,1,1,9,0)).kwh(5).build(),
                EnergyReading.builder().id(3L).cafe(cafe).timestamp(LocalDateTime.of(2025,1,2,10,0)).kwh(7).build()
        );
    }

    @Test
    void getDailyUsage_aggregatesByLocalDate() {
        when(energyReadingRepository.findByCafe_Id(anyLong())).thenReturn(sampleReadingsForDaily());

        Map<LocalDate, Double> daily = analyticsService.getDailyUsage(1L);

        assertThat(daily).hasSize(2);
        assertThat(daily.get(LocalDate.of(2025,1,1))).isEqualTo(15.0);
        assertThat(daily.get(LocalDate.of(2025,1,2))).isEqualTo(7.0);
    }

    @Test
    void getHourlyUsage_aggregatesByHour_andIncludesMissingHours() {
        // readings at 8h=10, 9h=5, 10h=7
        when(energyReadingRepository.findByCafe_Id(anyLong())).thenReturn(sampleReadingsForDaily());

        Map<Integer, Double> hourly = analyticsService.getHourlyUsage(1L);

        assertThat(hourly).hasSize(24);
        assertThat(hourly.get(8)).isEqualTo(10.0);
        assertThat(hourly.get(9)).isEqualTo(5.0);
        assertThat(hourly.get(10)).isEqualTo(7.0);
        // a missing hour should be present with 0.0
        assertThat(hourly.get(0)).isEqualTo(0.0);
        assertThat(hourly.get(23)).isEqualTo(0.0);
    }

    @Test
    void findPeaks_top3FallbackWhenNoStddevPeaks() {
        // Create simple dataset where mean+stddev threshold may not filter; peak fallback should return top 3 hours 10,9,8
        when(energyReadingRepository.findByCafe_Id(anyLong())).thenReturn(sampleReadingsForDaily());

        List<Integer> peaks = analyticsService.findPeaks(1L);
        // With values 8h=10, 9h=5, 10h=7 the descending order by kWh is [8,10,9]
        assertThat(peaks).containsExactly(8, 10, 9);
    }

    @Test
    void computeAnalytics_calculatesAveragesAndMax() {
        when(energyReadingRepository.findByCafe_Id(anyLong())).thenReturn(sampleReadingsForDaily());

        AnalyticsService.AnalyticsData data = analyticsService.computeAnalytics(1L);

        assertThat(data.cafeId()).isEqualTo(1L);
        assertThat(data.dailyUsage()).hasSize(2);
        assertThat(data.hourlyUsage()).hasSize(24);
        assertThat(data.peakHours()).hasSize(3);
        // average of [15, 7] = 11.0
        assertThat(data.averageDailyUsage()).isEqualTo(11.0);
        assertThat(data.maxDailyUsage()).isEqualTo(15.0);
    }
}
