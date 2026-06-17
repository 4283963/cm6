package com.crossborder.fx.controller;

import com.crossborder.fx.dto.ApiResponse;
import com.crossborder.fx.dto.SettlementRequestDTO;
import com.crossborder.fx.dto.SettlementResponseDTO;
import com.crossborder.fx.service.SettlementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settlement")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/apply")
    public ApiResponse<SettlementResponseDTO> applySettlement(@Valid @RequestBody SettlementRequestDTO request) {
        SettlementResponseDTO response = settlementService.createSettlementRequest(request);
        if (Boolean.FALSE.equals(response.getRiskCheckPassed())) {
            return ApiResponse.error(400, response.getRiskReason() != null ?
                    response.getRiskReason() : "风控校验未通过");
        }
        return ApiResponse.success("结汇申请已提交", response);
    }

    @GetMapping("/{requestNo}")
    public ApiResponse<SettlementResponseDTO> getSettlement(@PathVariable String requestNo) {
        SettlementResponseDTO response = settlementService.getSettlementByRequestNo(requestNo);
        if (response == null) {
            return ApiResponse.error(404, "结汇申请不存在");
        }
        return ApiResponse.success(response);
    }

    @GetMapping("/seller/{sellerId}")
    public ApiResponse<List<SettlementResponseDTO>> getSellerSettlements(@PathVariable String sellerId) {
        return ApiResponse.success(settlementService.getSettlementsBySeller(sellerId));
    }

    @GetMapping("/list")
    public ApiResponse<List<SettlementResponseDTO>> getAllSettlements() {
        return ApiResponse.success(settlementService.getAllSettlements());
    }
}
