package com.crossborder.fx.service;

import com.crossborder.fx.entity.IdempotentLog;
import com.crossborder.fx.repository.IdempotentLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class IdempotentService {

    private static final Logger log = LoggerFactory.getLogger(IdempotentService.class);

    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    public static final String TYPE_FX_CALLBACK = "FX_LOCK_CALLBACK";

    private final IdempotentLogRepository idempotentLogRepository;

    public IdempotentService(IdempotentLogRepository idempotentLogRepository) {
        this.idempotentLogRepository = idempotentLogRepository;
    }

    public static class AcquireResult {
        private final IdempotentLog logEntry;
        private final boolean newlyCreated;

        public AcquireResult(IdempotentLog logEntry, boolean newlyCreated) {
            this.logEntry = logEntry;
            this.newlyCreated = newlyCreated;
        }

        public IdempotentLog getLogEntry() { return logEntry; }
        public boolean isNewlyCreated() { return newlyCreated; }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AcquireResult tryAcquire(String idempotentToken, String businessType,
                                     String businessId, String requestBody) {
        Optional<IdempotentLog> existing = idempotentLogRepository.findByIdempotentToken(idempotentToken);
        if (existing.isPresent()) {
            log.debug("Idempotent token already exists: {}, status: {}, businessId: {}",
                    idempotentToken, existing.get().getStatus(), existing.get().getBusinessId());
            return new AcquireResult(existing.get(), false);
        }

        IdempotentLog logEntry = new IdempotentLog();
        logEntry.setIdempotentToken(idempotentToken);
        logEntry.setBusinessType(businessType);
        logEntry.setBusinessId(businessId);
        logEntry.setStatus(STATUS_PROCESSING);
        logEntry.setRequestBody(requestBody);

        try {
            IdempotentLog saved = idempotentLogRepository.save(logEntry);
            log.info("Acquired idempotent lock (new): token={}, type={}, businessId={}",
                    idempotentToken, businessType, businessId);
            return new AcquireResult(saved, true);
        } catch (DataIntegrityViolationException e) {
            log.warn("Idempotent token conflict (concurrent insert): {}", idempotentToken);
            IdempotentLog conflictEntry = idempotentLogRepository.findByIdempotentToken(idempotentToken)
                    .orElseThrow(() -> new IllegalStateException(
                            "Idempotent token conflict but not found: " + idempotentToken));
            return new AcquireResult(conflictEntry, false);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(String idempotentToken, String responseBody) {
        IdempotentLog logEntry = idempotentLogRepository.findByIdempotentToken(idempotentToken)
                .orElseThrow(() -> new IllegalArgumentException("Idempotent log not found: " + idempotentToken));
        logEntry.setStatus(STATUS_SUCCESS);
        logEntry.setResponseBody(responseBody);
        logEntry.setProcessedAt(LocalDateTime.now());
        idempotentLogRepository.save(logEntry);
        log.info("Idempotent log marked success: {}", idempotentToken);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String idempotentToken, String errorMessage) {
        IdempotentLog logEntry = idempotentLogRepository.findByIdempotentToken(idempotentToken)
                .orElseThrow(() -> new IllegalArgumentException("Idempotent log not found: " + idempotentToken));
        logEntry.setStatus(STATUS_FAILED);
        logEntry.setResponseBody(errorMessage);
        logEntry.setProcessedAt(LocalDateTime.now());
        idempotentLogRepository.save(logEntry);
        log.warn("Idempotent log marked failed: {}, reason: {}", idempotentToken, errorMessage);
    }

    public boolean isProcessed(String idempotentToken) {
        return idempotentLogRepository.findByIdempotentToken(idempotentToken)
                .map(log -> STATUS_SUCCESS.equals(log.getStatus()) || STATUS_FAILED.equals(log.getStatus()))
                .orElse(false);
    }

    public boolean isSuccess(String idempotentToken) {
        return idempotentLogRepository.findByIdempotentToken(idempotentToken)
                .map(log -> STATUS_SUCCESS.equals(log.getStatus()))
                .orElse(false);
    }

    public Optional<IdempotentLog> findLog(String idempotentToken) {
        return idempotentLogRepository.findByIdempotentToken(idempotentToken);
    }

    public <T> IdempotentResult<T> executeWithIdempotency(
            String idempotentToken,
            String businessType,
            String businessId,
            String requestBody,
            Supplier<T> action,
            Supplier<T> successResultSupplier) {

        AcquireResult acquireResult = tryAcquire(idempotentToken, businessType, businessId, requestBody);
        IdempotentLog logEntry = acquireResult.getLogEntry();
        String status = logEntry.getStatus();

        if (acquireResult.isNewlyCreated()) {
            log.debug("New idempotent token, proceeding with action: {}", idempotentToken);
            try {
                T result = action.get();
                markSuccess(idempotentToken, result != null ? result.toString() : "success");
                return IdempotentResult.success(result);
            } catch (Exception e) {
                markFailed(idempotentToken, e.getMessage());
                throw e;
            }
        }

        if (STATUS_SUCCESS.equals(status)) {
            log.info("Idempotent hit (SUCCESS) - returning cached result for token: {}", idempotentToken);
            return IdempotentResult.duplicate(successResultSupplier.get());
        }

        if (STATUS_PROCESSING.equals(status)) {
            log.warn("Idempotent hit (PROCESSING) - another request is in progress: {}", idempotentToken);
            return IdempotentResult.processing(successResultSupplier.get());
        }

        if (STATUS_FAILED.equals(status)) {
            log.warn("Idempotent hit (FAILED) - previous attempt failed for token: {}", idempotentToken);
            return IdempotentResult.failed(successResultSupplier.get());
        }

        return IdempotentResult.duplicate(successResultSupplier.get());
    }

    public static class IdempotentResult<T> {
        private final T data;
        private final boolean isDuplicate;
        private final boolean isProcessing;
        private final boolean isFailed;

        private IdempotentResult(T data, boolean isDuplicate, boolean isProcessing, boolean isFailed) {
            this.data = data;
            this.isDuplicate = isDuplicate;
            this.isProcessing = isProcessing;
            this.isFailed = isFailed;
        }

        public static <T> IdempotentResult<T> success(T data) {
            return new IdempotentResult<>(data, false, false, false);
        }

        public static <T> IdempotentResult<T> duplicate(T data) {
            return new IdempotentResult<>(data, true, false, false);
        }

        public static <T> IdempotentResult<T> processing(T data) {
            return new IdempotentResult<>(data, true, true, false);
        }

        public static <T> IdempotentResult<T> failed(T data) {
            return new IdempotentResult<>(data, true, false, true);
        }

        public T getData() { return data; }
        public boolean isDuplicate() { return isDuplicate; }
        public boolean isProcessing() { return isProcessing; }
        public boolean isFailed() { return isFailed; }
    }
}
