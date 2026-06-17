package com.crossborder.fx.controller;

import com.crossborder.fx.dto.ApiResponse;
import com.crossborder.fx.entity.QuoteInquiry;
import com.crossborder.fx.entity.RoutingDecision;
import com.crossborder.fx.repository.QuoteInquiryRepository;
import com.crossborder.fx.repository.RoutingDecisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private static final Logger log = LoggerFactory.getLogger(AuditController.class);

    private final QuoteInquiryRepository inquiryRepository;
    private final RoutingDecisionRepository decisionRepository;

    public AuditController(QuoteInquiryRepository inquiryRepository,
                           RoutingDecisionRepository decisionRepository) {
        this.inquiryRepository = inquiryRepository;
        this.decisionRepository = decisionRepository;
    }

    @GetMapping("/inquiries/by-request/{requestNo}")
    public ApiResponse<List<QuoteInquiry>> getInquiriesByRequestNo(@PathVariable String requestNo) {
        return ApiResponse.success(inquiryRepository.findByRequestNo(requestNo));
    }

    @GetMapping("/inquiries/by-inquiry/{inquiryId}")
    public ApiResponse<List<QuoteInquiry>> getInquiriesByInquiryId(@PathVariable String inquiryId) {
        return ApiResponse.success(inquiryRepository.findByInquiryId(inquiryId));
    }

    @GetMapping("/inquiries/by-bank/{bankCode}")
    public ApiResponse<List<QuoteInquiry>> getInquiriesByBankCode(@PathVariable String bankCode) {
        return ApiResponse.success(inquiryRepository.findByBankCode(bankCode));
    }

    @GetMapping("/decisions/by-request/{requestNo}")
    public ApiResponse<RoutingDecision> getDecisionByRequestNo(@PathVariable String requestNo) {
        return decisionRepository.findByRequestNo(requestNo)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "路由决策不存在"));
    }

    @GetMapping("/decisions/by-seller/{sellerId}")
    public ApiResponse<List<RoutingDecision>> getDecisionsBySeller(@PathVariable String sellerId) {
        return ApiResponse.success(decisionRepository.findBySellerId(sellerId));
    }

    @GetMapping("/decisions/{decisionId}")
    public ApiResponse<RoutingDecision> getDecisionById(@PathVariable String decisionId) {
        return decisionRepository.findByDecisionId(decisionId)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "路由决策不存在"));
    }
}
