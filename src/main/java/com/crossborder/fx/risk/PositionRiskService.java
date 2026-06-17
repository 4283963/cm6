package com.crossborder.fx.risk;

import com.crossborder.fx.common.Currency;
import com.crossborder.fx.common.SettlementStatus;
import com.crossborder.fx.dto.CurrencyExposureDTO;
import com.crossborder.fx.dto.RiskCheckResultDTO;
import com.crossborder.fx.dto.SettlementRequestDTO;
import com.crossborder.fx.entity.ExchangeRate;
import com.crossborder.fx.entity.PositionExposure;
import com.crossborder.fx.entity.Seller;
import com.crossborder.fx.entity.SettlementRequest;
import com.crossborder.fx.rate.ExchangeRateService;
import com.crossborder.fx.repository.PositionExposureRepository;
import com.crossborder.fx.repository.SellerRepository;
import com.crossborder.fx.repository.SettlementRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PositionRiskService {

    private static final Logger log = LoggerFactory.getLogger(PositionRiskService.class);

    private final ExchangeRateService rateService;
    private final PositionExposureRepository exposureRepository;
    private final SettlementRequestRepository settlementRepository;
    private final SellerRepository sellerRepository;

    @Value("${fx.risk.default-exposure-limit-cny:100000000}")
    private BigDecimal defaultExposureLimitCny;

    @Value("${fx.risk.circuit-breaker-threshold:0.9}")
    private BigDecimal circuitBreakerThreshold;

    public PositionRiskService(ExchangeRateService rateService,
                               PositionExposureRepository exposureRepository,
                               SettlementRequestRepository settlementRepository,
                               SellerRepository sellerRepository) {
        this.rateService = rateService;
        this.exposureRepository = exposureRepository;
        this.settlementRepository = settlementRepository;
        this.sellerRepository = sellerRepository;
    }

    @Transactional
    public RiskCheckResultDTO performRiskCheck(SettlementRequestDTO requestDTO) {
        log.info("Performing risk check for seller: {}, currency: {}, amount: {}",
                requestDTO.getSellerId(), requestDTO.getOriginalCurrency(), requestDTO.getOriginalAmount());

        RiskCheckResultDTO result = new RiskCheckResultDTO();
        result.setSellerId(requestDTO.getSellerId());
        result.setOriginalCurrency(requestDTO.getOriginalCurrency());
        result.setTargetCurrency(requestDTO.getTargetCurrency());
        result.setOriginalAmount(requestDTO.getOriginalAmount());
        result.setCheckTime(LocalDateTime.now());

        Seller seller = validateSeller(requestDTO.getSellerId(), result);
        if (seller == null) {
            return result;
        }

        ExchangeRate latestRate = rateService.getLatestRate(
                requestDTO.getOriginalCurrency(), requestDTO.getTargetCurrency());
        result.setExchangeRate(latestRate.getBuyPrice());
        result.setBuyPrice(latestRate.getBuyPrice());
        result.setSellPrice(latestRate.getSellPrice());
        result.setMidPrice(latestRate.getMidPrice());

        BigDecimal targetAmount = requestDTO.getOriginalAmount()
                .multiply(latestRate.getBuyPrice())
                .setScale(2, RoundingMode.HALF_UP);
        result.setTargetAmount(targetAmount);

        Map<Currency, CurrencyExposureDTO> exposures = calculateProjectedExposures(
                requestDTO.getOriginalCurrency(), requestDTO.getOriginalAmount(), targetAmount);
        result.setExposures(exposures);

        BigDecimal totalExposureCny = exposures.values().stream()
                .map(CurrencyExposureDTO::getExposureAmountCny)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.setTotalExposureCny(totalExposureCny);

        BigDecimal totalLimitCny = defaultExposureLimitCny.multiply(BigDecimal.valueOf(exposures.size()));
        result.setTotalExposureLimitCny(totalLimitCny);

        if (totalLimitCny.compareTo(BigDecimal.ZERO) > 0) {
            result.setExposureRatio(totalExposureCny.divide(totalLimitCny, 6, RoundingMode.HALF_UP));
        } else {
            result.setExposureRatio(BigDecimal.ZERO);
        }

        boolean circuitBreakerTriggered = exposures.values().stream()
                .anyMatch(e -> Boolean.TRUE.equals(e.getCircuitBreakerTriggered()));
        result.setCircuitBreakerTriggered(circuitBreakerTriggered);

        if (circuitBreakerTriggered) {
            result.setRiskCheckPassed(false);
            CurrencyBreakerInfo breaker = findTriggeredBreaker(exposures);
            result.setRiskReason(String.format(
                    "外汇头寸风险熔断触发: %s 敞口 %.2f CNY 已达限额 %.2f CNY 的 %.2f%%，超过阈值 %.0f%%",
                    breaker.currency,
                    breaker.exposureAmount,
                    breaker.exposureLimit,
                    breaker.exposureRatio.multiply(BigDecimal.valueOf(100)),
                    circuitBreakerThreshold.multiply(BigDecimal.valueOf(100))));
            log.warn("RISK REJECTED - Circuit breaker triggered: {}", result.getRiskReason());
        } else {
            result.setRiskCheckPassed(true);
            log.info("Risk check PASSED for seller {}, total exposure: {} CNY, ratio: {}",
                    requestDTO.getSellerId(), totalExposureCny, result.getExposureRatio());
        }

        return result;
    }

    @Transactional
    public void updatePositionAfterSettlement(SettlementRequest request) {
        if (request.getStatus() != SettlementStatus.APPROVED && request.getStatus() != SettlementStatus.COMPLETED) {
            return;
        }

        log.info("Updating position exposure after settlement: {}, amount: {} {}",
                request.getRequestNo(), request.getOriginalAmount(), request.getOriginalCurrency());

        PositionExposure exposure = exposureRepository.findByCurrency(request.getOriginalCurrency())
                .orElseGet(() -> createNewExposure(request.getOriginalCurrency()));

        exposure.setNetPosition(exposure.getNetPosition().add(request.getOriginalAmount()));

        ExchangeRate currentRate = rateService.getRate(request.getOriginalCurrency(), Currency.CNY);
        BigDecimal exposureCny = exposure.getNetPosition().multiply(currentRate.getMidPrice())
                .setScale(2, RoundingMode.HALF_UP);
        exposure.setExposureAmountCny(exposureCny);

        BigDecimal ratio = exposure.getExposureAmountCny()
                .divide(exposure.getExposureLimitCny(), 6, RoundingMode.HALF_UP);
        exposure.setExposureRatio(ratio);

        exposure.setIsCircuitBreakerTriggered(ratio.compareTo(circuitBreakerThreshold) >= 0);

        exposureRepository.save(exposure);

        log.info("Updated exposure for {}: net position={}, exposure CNY={}, ratio={}, circuit breaker={}",
                request.getOriginalCurrency(),
                exposure.getNetPosition(),
                exposure.getExposureAmountCny(),
                exposure.getExposureRatio(),
                exposure.getIsCircuitBreakerTriggered());
    }

    public Map<Currency, CurrencyExposureDTO> getCurrentExposures() {
        Map<Currency, CurrencyExposureDTO> result = new EnumMap<>(Currency.class);
        List<PositionExposure> exposures = exposureRepository.findAll();

        for (PositionExposure exp : exposures) {
            CurrencyExposureDTO dto = convertToDTO(exp);
            result.put(exp.getCurrency(), dto);
        }

        for (Currency ccy : new Currency[]{Currency.USD, Currency.EUR, Currency.GBP, Currency.JPY, Currency.HKD, Currency.AUD}) {
            if (!result.containsKey(ccy)) {
                PositionExposure exp = createNewExposure(ccy);
                result.put(ccy, convertToDTO(exp));
            }
        }

        return result;
    }

    public void resetExposure(Currency currency) {
        log.info("Resetting exposure for currency: {}", currency);
        Optional<PositionExposure> opt = exposureRepository.findByCurrency(currency);
        if (opt.isPresent()) {
            PositionExposure exposure = opt.get();
            exposure.setNetPosition(BigDecimal.ZERO);
            exposure.setExposureAmountCny(BigDecimal.ZERO);
            exposure.setExposureRatio(BigDecimal.ZERO);
            exposure.setIsCircuitBreakerTriggered(false);
            exposureRepository.save(exposure);
        }
    }

    private Seller validateSeller(String sellerId, RiskCheckResultDTO result) {
        Optional<Seller> sellerOpt = sellerRepository.findBySellerId(sellerId);
        if (sellerOpt.isEmpty()) {
            result.setRiskCheckPassed(false);
            result.setRiskReason("卖家不存在: " + sellerId);
            log.warn("Risk check failed - seller not found: {}", sellerId);
            return null;
        }

        Seller seller = sellerOpt.get();
        if (!Boolean.TRUE.equals(seller.getIsActive())) {
            result.setRiskCheckPassed(false);
            result.setRiskReason("卖家已被禁用: " + sellerId);
            log.warn("Risk check failed - seller inactive: {}", sellerId);
            return null;
        }
        return seller;
    }

    private Map<Currency, CurrencyExposureDTO> calculateProjectedExposures(
            Currency originalCurrency, BigDecimal originalAmount, BigDecimal targetAmountCny) {

        Map<Currency, CurrencyExposureDTO> result = new EnumMap<>(Currency.class);

        for (Currency ccy : new Currency[]{Currency.USD, Currency.EUR, Currency.GBP, Currency.JPY, Currency.HKD, Currency.AUD}) {
            PositionExposure existing = exposureRepository.findByCurrency(ccy)
                    .orElseGet(() -> createNewExposure(ccy));

            BigDecimal projectedNetPosition = existing.getNetPosition();
            if (ccy == originalCurrency) {
                projectedNetPosition = projectedNetPosition.add(originalAmount);
            }

            ExchangeRate rate = rateService.getRate(ccy, Currency.CNY);
            BigDecimal projectedExposureCny = projectedNetPosition.multiply(rate.getMidPrice())
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal projectedRatio = projectedExposureCny
                    .divide(existing.getExposureLimitCny(), 6, RoundingMode.HALF_UP);
            boolean breakerTriggered = projectedRatio.compareTo(circuitBreakerThreshold) >= 0;

            CurrencyExposureDTO dto = new CurrencyExposureDTO();
            dto.setCurrency(ccy);
            dto.setNetPosition(projectedNetPosition);
            dto.setExposureAmountCny(projectedExposureCny);
            dto.setExposureLimitCny(existing.getExposureLimitCny());
            dto.setExposureRatio(projectedRatio);
            dto.setCircuitBreakerTriggered(breakerTriggered);

            result.put(ccy, dto);

            log.debug("Projected exposure for {}: net={}, exposure CNY={}, ratio={}, breaker={}",
                    ccy, projectedNetPosition, projectedExposureCny, projectedRatio, breakerTriggered);
        }

        return result;
    }

    private PositionExposure createNewExposure(Currency currency) {
        PositionExposure exposure = new PositionExposure();
        exposure.setCurrency(currency);
        exposure.setNetPosition(BigDecimal.ZERO);
        exposure.setExposureAmountCny(BigDecimal.ZERO);
        exposure.setExposureLimitCny(defaultExposureLimitCny);
        exposure.setExposureRatio(BigDecimal.ZERO);
        exposure.setIsCircuitBreakerTriggered(false);
        return exposure;
    }

    private CurrencyExposureDTO convertToDTO(PositionExposure exp) {
        CurrencyExposureDTO dto = new CurrencyExposureDTO();
        dto.setCurrency(exp.getCurrency());
        dto.setNetPosition(exp.getNetPosition());
        dto.setExposureAmountCny(exp.getExposureAmountCny());
        dto.setExposureLimitCny(exp.getExposureLimitCny());
        dto.setExposureRatio(exp.getExposureRatio());
        dto.setCircuitBreakerTriggered(exp.getIsCircuitBreakerTriggered());
        return dto;
    }

    private static class CurrencyBreakerInfo {
        Currency currency;
        BigDecimal exposureAmount;
        BigDecimal exposureLimit;
        BigDecimal exposureRatio;
    }

    private CurrencyBreakerInfo findTriggeredBreaker(Map<Currency, CurrencyExposureDTO> exposures) {
        for (Map.Entry<Currency, CurrencyExposureDTO> entry : exposures.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue().getCircuitBreakerTriggered())) {
                CurrencyBreakerInfo info = new CurrencyBreakerInfo();
                info.currency = entry.getKey();
                info.exposureAmount = entry.getValue().getExposureAmountCny();
                info.exposureLimit = entry.getValue().getExposureLimitCny();
                info.exposureRatio = entry.getValue().getExposureRatio();
                return info;
            }
        }
        return null;
    }
}
