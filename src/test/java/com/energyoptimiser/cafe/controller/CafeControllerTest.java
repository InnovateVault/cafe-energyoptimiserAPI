package com.energyoptimiser.cafe.controller;

import com.energyoptimiser.cafe.dto.InsightsResponse;
import com.energyoptimiser.cafe.dto.UploadResponse;
import com.energyoptimiser.cafe.exception.GlobalExceptionHandler;
import com.energyoptimiser.cafe.service.AnalyticsService;
import com.energyoptimiser.cafe.service.OptimizationService;
import com.energyoptimiser.cafe.service.IngestionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CafeController.class)
@Import({CafeControllerTest.MockConfig.class, GlobalExceptionHandler.class})
class CafeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private OptimizationService optimizationService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        IngestionService ingestionService() {
            return Mockito.mock(IngestionService.class);
        }

        @Bean
        AnalyticsService analyticsService() {
            return Mockito.mock(AnalyticsService.class);
        }

        @Bean
        OptimizationService optimizationService() {
            return Mockito.mock(OptimizationService.class);
        }
    }

    @Test
    @DisplayName("POST /api/cafes/upload returns 200 and UploadResponse JSON")
    void upload_returnsUploadResponse() throws Exception {
        // Arrange
        MockMultipartFile csv = new MockMultipartFile(
                "file",
                "readings.csv",
                MediaType.TEXT_PLAIN_VALUE,
                ("""
                        name,location,timestamp,kwh
                        Cafe A,London,2025-01-31T10:00:00,1.2
                        """).getBytes()
        );

        UploadResponse response = new UploadResponse(1L, "readings.csv", 1, "OK", LocalDateTime.now());
        Mockito.when(ingestionService.processCSV(Mockito.any())).thenReturn(response);

        // Act + Assert
        mockMvc.perform(multipart("/api/cafes/upload").file(csv))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cafeId", is(1)))
                .andExpect(jsonPath("$.fileName", is("readings.csv")))
                .andExpect(jsonPath("$.rowsImported", is(1)))
                .andExpect(jsonPath("$.status", is("OK")))
                .andExpect(jsonPath("$.processedAt", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/cafes/upload without file returns 400 Bad Request")
    void upload_missingFile_returnsBadRequest() throws Exception
    {
        mockMvc.perform(multipart("/api/cafes/upload"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/cafes/{id}/insights returns 200 and InsightsResponse JSON")
    void insights_returnsInsightsResponse() throws Exception {
        // Arrange
        Long cafeId = 42L;

        // Use simple deterministic maps/lists for JSON assertions
        Map<Integer, Double> hourly = new TreeMap<>();
        hourly.put(9, 3.5);
        hourly.put(10, 4.0);

        Map<LocalDate, Double> daily = new TreeMap<>();
        daily.put(LocalDate.of(2025, 1, 31), 7.5);

        List<Integer> peaks = List.of(10);

        var analytics = new com.energyoptimiser.cafe.service.AnalyticsService.AnalyticsData(
                cafeId, hourly, daily, peaks, 7.5, 7.5
        );

        InsightsResponse insights = new InsightsResponse(hourly, daily, peaks, 7.5, 7.5,
                List.of("Consider shifting non-essential loads away from peak hour(s): [10]"));

        Mockito.when(analyticsService.computeAnalytics(cafeId)).thenReturn(analytics);
        Mockito.when(optimizationService.generateInsights(analytics)).thenReturn(insights);

        // Act + Assert
        mockMvc.perform(get("/api/cafes/{cafeId}/insights", cafeId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // top-level fields present
                .andExpect(jsonPath("$.hourlyUsage", aMapWithSize(2)))
                .andExpect(jsonPath("$.hourlyUsage['9']", is(closeTo(3.5, 0.0001))))
                .andExpect(jsonPath("$.hourlyUsage['10']", is(closeTo(4.0, 0.0001))))
                .andExpect(jsonPath("$.dailyUsage", aMapWithSize(1)))
                // LocalDate keys serialize as ISO strings like "2025-01-31"
                .andExpect(jsonPath("$.dailyUsage['2025-01-31']", is(closeTo(7.5, 0.0001))))
                .andExpect(jsonPath("$.peakHours", contains(10)))
                .andExpect(jsonPath("$.averageDailyUsage", is(closeTo(7.5, 0.0001))))
                .andExpect(jsonPath("$.maxDailyUsage", is(closeTo(7.5, 0.0001))))
                .andExpect(jsonPath("$.recommendations", hasSize(1)));
    }
}
