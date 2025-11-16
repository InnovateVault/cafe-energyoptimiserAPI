package com.energyoptimiser.cafe.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity representing a café profile.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "readings")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "CAFE_PROFILE")
public class CafeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    /**
     * All energy readings that belong to this café.
     * This is mainly kept so we can easily look up a café’s readings when needed
     * (for example, during debugging or analytics). It is loaded lazily, so it
     * won’t slow anything down unless we actually access it.
     */
    @OneToMany(mappedBy = "cafe", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<EnergyReading> readings = new HashSet<>();
}
