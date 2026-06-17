package com.crossborder.fx.repository;

import com.crossborder.fx.entity.SettlementRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRequestRepository extends JpaRepository<SettlementRequest, Long> {

    List<SettlementRequest> findBySellerId(String sellerId);

    SettlementRequest findByRequestNo(String requestNo);
}
