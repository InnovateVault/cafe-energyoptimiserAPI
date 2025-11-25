package com.energyoptimiser.cafe.controller;

import com.energyoptimiser.cafe.dto.InsightsResponse;
import com.energyoptimiser.cafe.dto.UploadResponse;
import com.energyoptimiser.cafe.service.AnalyticsService;
import com.energyoptimiser.cafe.service.IngestionService;
import com.energyoptimiser.cafe.service.OptimizationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller exposing endpoints for CSV ingestion and insight retrieval.
 */
@RestController
@RequestMapping("/api/cafes")
public class CafeController {

    private final IngestionService ingestionService;
    private final AnalyticsService analyticsService;
    private final OptimizationService optimizationService;

    public CafeController(IngestionService ingestionService,
                          AnalyticsService analyticsService,
                          OptimizationService optimizationService) {
        this.ingestionService = ingestionService;
        this.analyticsService = analyticsService;
        this.optimizationService = optimizationService;
    }

    /**
     * Upload a CSV file with columns: name,location, timestamp, kwh
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(@RequestParam("file") MultipartFile file) {
        // null and empty check handled in IngestionService
        return ingestionService.processCSV(file);
    }

    /**
     * Retrieve computed insights for a given café id.
     */
    @GetMapping("/{cafeId}/insights")
    public InsightsResponse getInsights(@PathVariable("cafeId") Long cafeId) {
        // null and not-found checks handled in service layer / exception handler
        var analytics = analyticsService.computeAnalytics(cafeId);
        return optimizationService.generateInsights(analytics);
    }
}