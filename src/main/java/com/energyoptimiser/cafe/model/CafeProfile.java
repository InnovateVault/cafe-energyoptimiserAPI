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

    @OneToMany(mappedBy = "cafe", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<EnergyReading> readings = new HashSet<>();
}
