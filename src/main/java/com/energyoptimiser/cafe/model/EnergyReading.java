package com.energyoptimiser.cafe.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing an energy reading for a café.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "cafe")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "ENERGY_READING")
public class EnergyReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafe_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_energy_reading_cafe"))
    private CafeProfile cafe;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private double kwh;
}
