package com.crossborder.fx.service;

import com.crossborder.fx.common.Currency;
import com.crossborder.fx.common.SettlementStatus;
import com.crossborder.fx.dto.FxLockCallbackDTO;
import com.crossborder.fx.dto.RiskCheckResultDTO;
import com.crossborder.fx.dto.SettlementRequestDTO;
import com.crossborder.fx.dto.SettlementResponseDTO;
import com.crossborder.fx.entity.SettlementRequest;
import com.crossborder.fx.repository.RoutingDecisionRepository;
import com.crossborder.fx.repository.SettlementRequestRepository;
import com.crossborder.fx.risk.PositionRiskService;
import com.crossborder.fx.service.IdempotentService.IdempotentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final SettlementRequestRepository settlementRepository;
    private final PositionRiskService positionRiskService;
    private final IdempotentService idempotentService;
    private final RoutingDecisionRepository routingDecisionRepository;

    @Value("${fx.settlement.instant-mode:false}")
    private boolean instantMode;

    public SettlementService(SettlementRequestRepository settlementRepository,
                             PositionRiskService positionRiskService,
                             IdempotentService idempotentService,
                             RoutingDecisionRepository routingDecisionRepository) {
        this.settlementRepository = settlementRepository;
        this.positionRiskService = positionRiskService;
        this.idempotentService = idempotentService;
        this.routingDecisionRepository = routingDecisionRepository;
    }

    @Transactional
    public SettlementResponseDTO createSettlementRequest(SettlementRequestDTO requestDTO) {
        log.info("Creating settlement request for seller: {}, amount: {} {}",
                requestDTO.getSellerId(), requestDTO.getOriginalAmount(), requestDTO.getOriginalCurrency());

        if (requestDTO.getTargetCurrency() == null) {
            requestDTO.setTargetCurrency(Currency.CNY);
        }

        RiskCheckResultDTO riskResult = positionRiskService.performRiskCheck(requestDTO);

        SettlementRequest request = new SettlementRequest();
        request.setRequestNo(generateRequestNo());
        request.setSellerId(requestDTO.getSellerId());
        request.setOriginalCurrency(requestDTO.getOriginalCurrency());
        request.setTargetCurrency(requestDTO.getTargetCurrency());
        request.setOriginalAmount(requestDTO.getOriginalAmount());
        request.setExchangeRate(riskResult.getExchangeRate());
        request.setTargetAmount(riskResult.getTargetAmount());
        request.setRiskCheckPassed(riskResult.isRiskCheckPassed());
        request.setRiskReason(riskResult.getRiskReason());

        if (!riskResult.isRiskCheckPassed()) {
            request.setStatus(SettlementStatus.RISK_REJECTED);
            log.warn("Settlement request RISK_REJECTED: {}. Reason: {}",
                    request.getRequestNo(), riskResult.getRiskReason());
        } else if (instantMode) {
            request.setStatus(SettlementStatus.COMPLETED);
            positionRiskService.updatePositionAfterSettlement(request);
            log.info("Settlement request COMPLETED (instant mode): {}", request.getRequestNo());
        } else {
            request.setStatus(SettlementStatus.PENDING);
            log.info("Settlement request PENDING (waiting for lock callback): {}", request.getRequestNo());
        }

        settlementRepository.save(request);
        return convertToResponseDTO(request);
    }

    @Transactional
    public CallbackResult processFxLockCallback(FxLockCallbackDTO callbackDTO) {
        String idempotentToken = callbackDTO.getIdempotentToken();
        String requestNo = callbackDTO.getRequestNo();

        log.info("Processing FX lock callback: token={}, requestNo={}, status={}",
                idempotentToken, requestNo, callbackDTO.getStatus());

        IdempotentResult<SettlementResponseDTO> idempotentResult = idempotentService.executeWithIdempotency(
                idempotentToken,
                IdempotentService.TYPE_FX_CALLBACK,
                requestNo,
                callbackDTO.toString(),
                () -> doProcessCallback(callbackDTO),
                () -> getSettlementByRequestNo(requestNo)
        );

        SettlementResponseDTO data = idempotentResult.getData();
        if (idempotentResult.isDuplicate() && !idempotentResult.isProcessing()) {
            log.info("Idempotent callback hit - returning cached result for token: {}", idempotentToken);
            return CallbackResult.duplicate(data);
        }

        if (idempotentResult.isProcessing()) {
            log.warn("Idempotent callback hit - request is still processing: {}", idempotentToken);
            return CallbackResult.processing(data);
        }

        return CallbackResult.success(data);
    }

    private SettlementResponseDTO doProcessCallback(FxLockCallbackDTO callbackDTO) {
        String requestNo = callbackDTO.getRequestNo();
        SettlementRequest request = settlementRepository.findByRequestNo(requestNo);

        if (request == null) {
            throw new IllegalArgumentException("结汇申请不存在: " + requestNo);
        }

        if (request.getStatus() == SettlementStatus.COMPLETED) {
            log.warn("Settlement already COMPLETED, skipping: {}", requestNo);
            return convertToResponseDTO(request);
        }

        if (request.getStatus() != SettlementStatus.PENDING
                && request.getStatus() != SettlementStatus.LOCK_FAILED) {
            throw new IllegalStateException("结汇申请状态不允许回调，当前状态: " + request.getStatus());
        }

        String callbackStatus = callbackDTO.getStatus();

        if ("SUCCESS".equalsIgnoreCase(callbackStatus) || "LOCK_SUCCESS".equalsIgnoreCase(callbackStatus)) {
            return handleLockSuccess(request, callbackDTO);
        } else if ("FAILED".equalsIgnoreCase(callbackStatus) || "LOCK_FAILED".equalsIgnoreCase(callbackStatus)) {
            return handleLockFailed(request, callbackDTO);
        } else {
            throw new IllegalArgumentException("未知的回调状态: " + callbackStatus);
        }
    }

    private SettlementResponseDTO handleLockSuccess(SettlementRequest request, FxLockCallbackDTO callbackDTO) {
        log.info("Lock success for request: {}, lock rate: {}, amount: {}",
                request.getRequestNo(), callbackDTO.getLockRate(), callbackDTO.getLockAmount());

        if (callbackDTO.getLockRate() != null) {
            request.setExchangeRate(callbackDTO.getLockRate());
        }
        if (callbackDTO.getSettlementAmount() != null) {
            request.setTargetAmount(callbackDTO.getSettlementAmount());
        } else if (callbackDTO.getLockAmount() != null && callbackDTO.getLockRate() != null) {
            request.setTargetAmount(callbackDTO.getLockAmount().multiply(callbackDTO.getLockRate()));
        }

        request.setStatus(SettlementStatus.COMPLETED);

        positionRiskService.updatePositionAfterSettlement(request);

        settlementRepository.save(request);

        log.info("Settlement COMPLETED: {}, seller: {}, target amount: {} {}",
                request.getRequestNo(), request.getSellerId(),
                request.getTargetAmount(), request.getTargetCurrency());

        return convertToResponseDTO(request);
    }

    private SettlementResponseDTO handleLockFailed(SettlementRequest request, FxLockCallbackDTO callbackDTO) {
        log.warn("Lock failed for request: {}, reason: {}",
                request.getRequestNo(), callbackDTO.getRemark());

        request.setStatus(SettlementStatus.LOCK_FAILED);
        request.setRiskReason(callbackDTO.getRemark());

        settlementRepository.save(request);

        return convertToResponseDTO(request);
    }

    public SettlementResponseDTO getSettlementByRequestNo(String requestNo) {
        SettlementRequest request = settlementRepository.findByRequestNo(requestNo);
        if (request == null) {
            return null;
        }
        return convertToResponseDTO(request);
    }

    public List<SettlementResponseDTO> getSettlementsBySeller(String sellerId) {
        return settlementRepository.findBySellerId(sellerId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<SettlementResponseDTO> getAllSettlements() {
        return settlementRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    private String generateRequestNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "FX" + datePart + randomPart;
    }

    private SettlementResponseDTO convertToResponseDTO(SettlementRequest request) {
        SettlementResponseDTO dto = new SettlementResponseDTO();
        dto.setRequestNo(request.getRequestNo());
        dto.setSellerId(request.getSellerId());
        dto.setOriginalCurrency(request.getOriginalCurrency().getCode());
        dto.setTargetCurrency(request.getTargetCurrency().getCode());
        dto.setOriginalAmount(request.getOriginalAmount());
        dto.setTargetAmount(request.getTargetAmount());
        dto.setExchangeRate(request.getExchangeRate());
        dto.setStatus(request.getStatus());
        dto.setRiskCheckPassed(request.getRiskCheckPassed());
        dto.setRiskReason(request.getRiskReason());
        dto.setCreatedAt(request.getCreatedAt());
        routingDecisionRepository.findByRequestNo(request.getRequestNo()).ifPresent(decision -> {
            dto.setSelectedBankCode(decision.getSelectedBankCode());
            dto.setSelectedBankName(decision.getSelectedBankName());
            dto.setInquiryId(decision.getInquiryId());
        });
        return dto;
    }

    public static class CallbackResult {
        private final SettlementResponseDTO data;
        private final boolean isDuplicate;
        private final boolean isProcessing;

        private CallbackResult(SettlementResponseDTO data, boolean isDuplicate, boolean isProcessing) {
            this.data = data;
            this.isDuplicate = isDuplicate;
            this.isProcessing = isProcessing;
        }

        public static CallbackResult success(SettlementResponseDTO data) {
            return new CallbackResult(data, false, false);
        }

        public static CallbackResult duplicate(SettlementResponseDTO data) {
            return new CallbackResult(data, true, false);
        }

        public static CallbackResult processing(SettlementResponseDTO data) {
            return new CallbackResult(data, true, true);
        }

        public SettlementResponseDTO getData() { return data; }
        public boolean isDuplicate() { return isDuplicate; }
        public boolean isProcessing() { return isProcessing; }
    }
}
