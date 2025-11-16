package com.energyoptimiser.cafe.repository;

import com.energyoptimiser.cafe.model.CafeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CafeProfileRepository extends JpaRepository<CafeProfile, Long> {

    Optional<CafeProfile> findByNameAndLocation(String name, String location);
}
