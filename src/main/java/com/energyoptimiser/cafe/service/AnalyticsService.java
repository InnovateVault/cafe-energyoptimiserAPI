package com.energyoptimiser.cafe.service;

import com.energyoptimiser.cafe.exception.CafeNotFoundException;
import com.energyoptimiser.cafe.model.EnergyReading;
import com.energyoptimiser.cafe.repository.EnergyReadingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService
{

    private final EnergyReadingRepository energyReadingRepository;

    public AnalyticsService(EnergyReadingRepository energyReadingRepository) {
        this.energyReadingRepository = energyReadingRepository;
    }

    /**
     * Aggregate total kWh per day for a café.
     */
    public Map<LocalDate, Double> getDailyUsage(Long cafeId) {
        List<EnergyReading> readings = energyReadingRepository.findByCafe_Id(cafeId);
        if (readings.isEmpty()) {
            throw new CafeNotFoundException(cafeId);
        }

        Map<LocalDate, Double> daily = new TreeMap<>();
        for (EnergyReading r : readings) {
            LocalDate day = r.getTimestamp().toLocalDate();
            daily.merge(day, r.getKwh(), Double::sum);
        }
        return daily;
    }

    /**
     * Aggregate total kWh per hour-of-day (0-23) for a café.
     */
    public Map<Integer, Double> getHourlyUsage(Long cafeId) {
        List<EnergyReading> readings = energyReadingRepository.findByCafe_Id(cafeId);
        if (readings.isEmpty()) {
            throw new CafeNotFoundException(cafeId);
        }

        Map<Integer, Double> hourly = new TreeMap<>();
        for (EnergyReading r : readings) {
            int hour = r.getTimestamp().getHour();
            hourly.merge(hour, r.getKwh(), Double::sum);
        }
        // ensure all 24 hours exist (optional)
        for (int h = 0; h < 24; h++) {
            hourly.putIfAbsent(h, 0.0);
        }
        return hourly;
    }

    /**
     * Identify peak hours as the top N hours by usage (default 3) or those above mean+std dev.
     */
    public List<Integer> findPeaks(Long cafeId) {
        Map<Integer, Double> hourly = getHourlyUsage(cafeId);
        if (hourly.isEmpty()) return List.of();

        double mean = hourly.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = hourly.values().stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0.0);
        double stddev = Math.sqrt(variance);
        double threshold = mean + stddev; // simple z=1 threshold

        List<Integer> peaks = hourly.entrySet().stream()
                .filter(e -> e.getValue() >= threshold)
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // if none met a threshold, fallback to the top 3 by value
        if (peaks.isEmpty()) {
            peaks = hourly.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }
        return peaks;
    }

    /**
     * Build analytics data snapshot consumed by OptimizationService.
     */
    public AnalyticsData computeAnalytics(Long cafeId) {
        Map<Integer, Double> hourly = getHourlyUsage(cafeId);
        Map<LocalDate, Double> daily = getDailyUsage(cafeId);
        List<Integer> peaks = findPeaks(cafeId);

        double averageDaily = daily.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double maxDaily = daily.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        return new AnalyticsData(cafeId, hourly, daily, peaks, averageDaily, maxDaily);
    }

    /**
     * Internal immutable analytics bundle.
     */
    public record AnalyticsData(
            Long cafeId,
            Map<Integer, Double> hourlyUsage,
            Map<LocalDate, Double> dailyUsage,
            List<Integer> peakHours,
            double averageDailyUsage,
            double maxDailyUsage) {
    }
}
