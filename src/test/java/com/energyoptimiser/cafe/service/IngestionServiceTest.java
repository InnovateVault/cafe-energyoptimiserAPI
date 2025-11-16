package com.energyoptimiser.cafe.service;

import com.energyoptimiser.cafe.dto.UploadResponse;
import com.energyoptimiser.cafe.model.CafeProfile;
import com.energyoptimiser.cafe.model.EnergyReading;
import com.energyoptimiser.cafe.repository.CafeProfileRepository;
import com.energyoptimiser.cafe.repository.EnergyReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IngestionServiceTest {

    private CafeProfileRepository cafeRepo;
    private EnergyReadingRepository readingRepo;
    private IngestionService ingestionService;

    @BeforeEach
    void setUp() {
        cafeRepo = Mockito.mock(CafeProfileRepository.class);
        readingRepo = Mockito.mock(EnergyReadingRepository.class);
        ingestionService = new IngestionService(cafeRepo, readingRepo);
    }

    @Test
    void processCSV_savesReadings_andCreatesCafeOnce() {
        String csv = """
                name,location,timestamp,kwh
                Cafe A,Loc,2025-01-01T08:00:00,1.5
                Cafe A,Loc,2025-01-01T09:00:00,2.5
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "readings.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        CafeProfile savedCafe = CafeProfile.builder().id(42L).name("Cafe A").location("Loc").build();
        when(cafeRepo.findByNameAndLocation(eq("Cafe A"), eq("Loc")))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedCafe));
        when(cafeRepo.save(any(CafeProfile.class))).thenReturn(savedCafe);

        UploadResponse response = ingestionService.processCSV(file);

        assertThat(response.cafeId()).isEqualTo(42L);
        assertThat(response.fileName()).isEqualTo("readings.csv");
        assertThat(response.rowsImported()).isEqualTo(2);
        assertThat(response.status()).isEqualTo("OK");
        assertThat(response.processedAt()).isNotNull();

        // Café looked up twice (once per row) and saved once
        verify(cafeRepo, times(2)).findByNameAndLocation("Cafe A", "Loc");
        verify(cafeRepo, times(1)).save(any(CafeProfile.class));
        // Two readings saved
        verify(readingRepo, times(2)).save(any(EnergyReading.class));

        // Capture a reading and assert parsed values
        ArgumentCaptor<EnergyReading> captor = ArgumentCaptor.forClass(EnergyReading.class);
        verify(readingRepo, atLeastOnce()).save(captor.capture());
        List<EnergyReading> saved = captor.getAllValues();
        assertThat(saved.getFirst().getTimestamp()).isEqualTo(LocalDateTime.parse("2025-01-01T08:00:00"));
        assertThat(saved.getFirst().getKwh()).isEqualTo(1.5);
    }

    @Test
    void processCSV_handlesMalformedRow_setsErrorAndZeroImports() {
        String csv = "name,location,timestamp,kwh\n" +
                // malformed: missing kwh
                "Cafe A,Loc,2025-01-01T08:00:00\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        UploadResponse response = ingestionService.processCSV(file);

        assertThat(response.rowsImported()).isEqualTo(0);
        assertThat(response.status()).startsWith("ERROR:");

        verifyNoInteractions(readingRepo);
    }

    @Test
    void processCSV_reusesExistingCafe_withoutSavingNew() {
        String csv = """
                name,location,timestamp,kwh
                Cafe B,Loc,2025-01-01T10:00:00,3
                Cafe B,Loc,2025-01-01T11:00:00,4
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "existing.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        CafeProfile existing = CafeProfile.builder().id(7L).name("Cafe B").location("Loc").build();
        when(cafeRepo.findByNameAndLocation("Cafe B", "Loc")).thenReturn(Optional.of(existing));

        UploadResponse response = ingestionService.processCSV(file);

        assertThat(response.cafeId()).isEqualTo(7L);
        assertThat(response.rowsImported()).isEqualTo(2);
        verify(cafeRepo, never()).save(any());
        verify(readingRepo, times(2)).save(any(EnergyReading.class));
    }
}
