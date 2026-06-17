package com.crossborder.fx.repository;

import com.crossborder.fx.entity.IdempotentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotentLogRepository extends JpaRepository<IdempotentLog, Long> {

    Optional<IdempotentLog> findByIdempotentToken(String idempotentToken);

    boolean existsByIdempotentToken(String idempotentToken);
}
