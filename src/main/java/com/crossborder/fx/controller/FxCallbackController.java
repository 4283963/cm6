package com.crossborder.fx.controller;

import com.crossborder.fx.dto.ApiResponse;
import com.crossborder.fx.dto.FxLockCallbackDTO;
import com.crossborder.fx.dto.SettlementResponseDTO;
import com.crossborder.fx.service.SettlementService;
import com.crossborder.fx.service.SettlementService.CallbackResult;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/callback")
public class FxCallbackController {

    private static final Logger log = LoggerFactory.getLogger(FxCallbackController.class);

    private final SettlementService settlementService;

    public FxCallbackController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/fx-lock")
    public ResponseEntity<ApiResponse<SettlementResponseDTO>> handleFxLockCallback(
            @Valid @RequestBody FxLockCallbackDTO callbackDTO) {

        log.info("Received FX lock callback: token={}, requestNo={}, status={}",
                callbackDTO.getIdempotentToken(),
                callbackDTO.getRequestNo(),
                callbackDTO.getStatus());

        try {
            CallbackResult result = settlementService.processFxLockCallback(callbackDTO);

            SettlementResponseDTO data = result.getData();

            if (result.isDuplicate() && !result.isProcessing()) {
                log.info("Duplicate callback, returning same result: token={}", callbackDTO.getIdempotentToken());
                return ResponseEntity.status(HttpStatus.OK)
                        .body(ApiResponse.success("回调重复，返回上次处理结果(幂等)", data));
            }

            if (result.isProcessing()) {
                log.warn("Callback still processing: token={}", callbackDTO.getIdempotentToken());
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                        .body(ApiResponse.error(202, "请求处理中，请稍后再试"));
            }

            log.info("Callback processed successfully: token={}, requestNo={}",
                    callbackDTO.getIdempotentToken(), callbackDTO.getRequestNo());
            return ResponseEntity.ok(ApiResponse.success("回调处理成功", data));

        } catch (IllegalArgumentException e) {
            log.warn("Callback validation failed: token={}, reason={}", callbackDTO.getIdempotentToken(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Callback state error: token={}, reason={}", callbackDTO.getIdempotentToken(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(409, e.getMessage()));
        } catch (Exception e) {
            log.error("Callback processing failed: token={}", callbackDTO.getIdempotentToken(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "回调处理失败: " + e.getMessage()));
        }
    }

    @GetMapping("/fx-lock/{idempotentToken}")
    public ApiResponse<Boolean> checkCallbackProcessed(@PathVariable String idempotentToken) {
        boolean processed = settlementService != null;
        return ApiResponse.success(processed);
    }
}
