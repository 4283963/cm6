package com.crossborder.fx.service;

import com.crossborder.fx.common.Currency;
import com.crossborder.fx.common.SettlementStatus;
import com.crossborder.fx.dto.RiskCheckResultDTO;
import com.crossborder.fx.dto.SettlementRequestDTO;
import com.crossborder.fx.dto.SettlementResponseDTO;
import com.crossborder.fx.entity.SettlementRequest;
import com.crossborder.fx.repository.SettlementRequestRepository;
import com.crossborder.fx.risk.PositionRiskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public SettlementService(SettlementRequestRepository settlementRepository,
                             PositionRiskService positionRiskService) {
        this.settlementRepository = settlementRepository;
        this.positionRiskService = positionRiskService;
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

        if (riskResult.isRiskCheckPassed()) {
            request.setStatus(SettlementStatus.APPROVED);
            positionRiskService.updatePositionAfterSettlement(request);
            log.info("Settlement request APPROVED: {}", request.getRequestNo());
        } else {
            request.setStatus(SettlementStatus.RISK_REJECTED);
            log.warn("Settlement request REJECTED due to risk: {}. Reason: {}",
                    request.getRequestNo(), riskResult.getRiskReason());
        }

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
        return dto;
    }
}
