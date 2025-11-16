package com.energyoptimiser.cafe.repository;

import com.energyoptimiser.cafe.model.EnergyReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnergyReadingRepository extends JpaRepository<EnergyReading, Long> {

    List<EnergyReading> findByCafe_Id(Long cafeId);
}
