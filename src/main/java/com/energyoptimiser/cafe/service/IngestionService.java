package com.energyoptimiser.cafe.service;

import com.energyoptimiser.cafe.dto.UploadResponse;
import com.energyoptimiser.cafe.exception.BadRequestException;
import com.energyoptimiser.cafe.model.CafeProfile;
import com.energyoptimiser.cafe.model.EnergyReading;
import com.energyoptimiser.cafe.repository.CafeProfileRepository;
import com.energyoptimiser.cafe.repository.EnergyReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class IngestionService {

    private final CafeProfileRepository cafeProfileRepository;
    private final EnergyReadingRepository energyReadingRepository;

    public IngestionService(CafeProfileRepository cafeProfileRepository,
                            EnergyReadingRepository energyReadingRepository) {
        this.cafeProfileRepository = cafeProfileRepository;
        this.energyReadingRepository = energyReadingRepository;
    }

    /**
     * Process a CSV file with columns: name,location, timestamp, kwh
     * - Finds or creates the café profile
     * - Persists energy readings
     */
    @Transactional
    public UploadResponse processCSV(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }

        int imported = 0;
        String status = "OK";
        Long cafeId = null;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = br.readLine();
            if (header == null || !header.equals("name,location,timestamp,kwh")) {
                throw new BadRequestException(
                        "CSV header does not match expected columns. Expected: name,location,timestamp,kwh. Found: " + header);
            }  // Read the first line (CSV header) and validate that it matches expected columns
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                Row row = parseRow(line);
                CafeProfile cafe = findOrCreateCafe(row.name, row.location);
                if (cafeId == null) cafeId = cafe.getId();

                EnergyReading reading = EnergyReading.builder()
                        .cafe(cafe)
                        .timestamp(row.timestamp)
                        .kwh(row.kwh)
                        .build();
                energyReadingRepository.save(reading);
                imported++;
            }
        } catch (Exception e) {
            throw new BadRequestException("Failed to process CSV: " + e.getMessage());
        }

        return new UploadResponse(cafeId, file.getOriginalFilename(), imported, status, java.time.LocalDateTime.now());
    }

    private CafeProfile findOrCreateCafe(String name, String location) {
        return cafeProfileRepository.findByNameAndLocation(name, location)
                .orElseGet(() -> cafeProfileRepository.save(CafeProfile.builder()
                        .name(name)
                        .location(location)
                        .build()));
    }

    /**
     * Parse a CSV row into strongly typed values. Expected format:
     * name,location, timestamp, kwh
     * timestamp example: 2025-01-31T14:00:00
     */
    private Row parseRow(String csvLine) {
        String[] parts = csvLine.split(",");
        if (parts.length < 4) {
            throw new BadRequestException("Invalid CSV row: " + csvLine);
        }
        String name = parts[0].trim();
        String location = parts[1].trim();
        String ts = parts[2].trim();
        String kwhStr = parts[3].trim();

        LocalDateTime timestamp = LocalDateTime.parse(ts, DateTimeFormatter.ISO_DATE_TIME);
        double kwh = Double.parseDouble(kwhStr);
        return new Row(name, location, timestamp, kwh);
    }

    private record Row(String name, String location, LocalDateTime timestamp, double kwh) {}
}
