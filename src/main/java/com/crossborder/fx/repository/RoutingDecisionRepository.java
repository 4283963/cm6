package com.crossborder.fx.repository;

import com.crossborder.fx.entity.RoutingDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoutingDecisionRepository extends JpaRepository<RoutingDecision, Long> {

    Optional<RoutingDecision> findByRequestNo(String requestNo);

    Optional<RoutingDecision> findByDecisionId(String decisionId);

    List<RoutingDecision> findBySellerId(String sellerId);
}
