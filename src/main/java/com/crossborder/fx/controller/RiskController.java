package com.crossborder.fx.controller;

import com.crossborder.fx.common.Currency;
import com.crossborder.fx.dto.ApiResponse;
import com.crossborder.fx.dto.CurrencyExposureDTO;
import com.crossborder.fx.dto.RiskCheckResultDTO;
import com.crossborder.fx.dto.SettlementRequestDTO;
import com.crossborder.fx.entity.ExchangeRate;
import com.crossborder.fx.rate.ExchangeRateService;
import com.crossborder.fx.risk.PositionRiskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/risk")
public class RiskController {

    private final PositionRiskService positionRiskService;
    private final ExchangeRateService rateService;

    public RiskController(PositionRiskService positionRiskService,
                          ExchangeRateService rateService) {
        this.positionRiskService = positionRiskService;
        this.rateService = rateService;
    }

    @PostMapping("/check")
    public ApiResponse<RiskCheckResultDTO> checkRisk(@Valid @RequestBody SettlementRequestDTO request) {
        RiskCheckResultDTO result = positionRiskService.performRiskCheck(request);
        if (!result.isRiskCheckPassed()) {
            return ApiResponse.error(400, result.getRiskReason());
        }
        return ApiResponse.success("风控检查通过", result);
    }

    @GetMapping("/exposures")
    public ApiResponse<Map<Currency, CurrencyExposureDTO>> getAllExposures() {
        return ApiResponse.success(positionRiskService.getCurrentExposures());
    }

    @GetMapping("/rates")
    public ApiResponse<List<ExchangeRate>> getCurrentRates() {
        return ApiResponse.success(rateService.getAllCurrentRates());
    }

    @GetMapping("/rate/{base}/{quote}")
    public ApiResponse<ExchangeRate> getRate(@PathVariable Currency base, @PathVariable Currency quote) {
        return ApiResponse.success(rateService.getRate(base, quote));
    }

    @PostMapping("/exposures/reset/{currency}")
    public ApiResponse<String> resetExposure(@PathVariable Currency currency) {
        positionRiskService.resetExposure(currency);
        return ApiResponse.success("已重置 " + currency + " 的头寸敞口", null);
    }

    @PostMapping("/rates/refresh")
    public ApiResponse<String> refreshRates() {
        rateService.refreshAllRates();
        return ApiResponse.success("汇率刷新完成", null);
    }
}
