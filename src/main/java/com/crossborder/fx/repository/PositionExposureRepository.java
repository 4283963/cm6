package com.crossborder.fx.repository;

import com.crossborder.fx.common.Currency;
import com.crossborder.fx.entity.PositionExposure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PositionExposureRepository extends JpaRepository<PositionExposure, Long> {

    Optional<PositionExposure> findByCurrency(Currency currency);
}
