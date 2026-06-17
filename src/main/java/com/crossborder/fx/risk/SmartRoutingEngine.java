package com.crossborder.fx.risk;

import com.crossborder.fx.common.Currency;
import com.crossborder.fx.dto.BankQuoteDTO;
import com.crossborder.fx.dto.MultiBankQuoteResultDTO;
import com.crossborder.fx.entity.QuoteInquiry;
import com.crossborder.fx.entity.RoutingDecision;
import com.crossborder.fx.rate.BankQuote;
import com.crossborder.fx.rate.MultiBankQuoteService;
import com.crossborder.fx.repository.QuoteInquiryRepository;
import com.crossborder.fx.repository.RoutingDecisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class SmartRoutingEngine {

    private static final Logger log = LoggerFactory.getLogger(SmartRoutingEngine.class);

    private final MultiBankQuoteService quoteService;
    private final QuoteInquiryRepository inquiryRepository;
    private final RoutingDecisionRepository decisionRepository;

    public SmartRoutingEngine(MultiBankQuoteService quoteService,
                              QuoteInquiryRepository inquiryRepository,
                              RoutingDecisionRepository decisionRepository) {
        this.quoteService = quoteService;
        this.inquiryRepository = inquiryRepository;
        this.decisionRepository = decisionRepository;
    }

    /**
     * 执行多银行询价 + 智能路由决策
     */
    @Transactional
    public MultiBankQuoteResultDTO executeRouting(String requestNo, String sellerId,
                                                    Currency originalCurrency, Currency targetCurrency,
                                                    BigDecimal originalAmount) {
        String inquiryId = generateInquiryId();
        log.info("Executing smart routing: inquiryId={}, requestNo={}, seller={}, {} {} -> {}",
                inquiryId, requestNo, sellerId, originalAmount, originalCurrency, targetCurrency);

        // 1. 异步并发询价
        List<BankQuote> quotes = quoteService.fetchQuotesAsync(originalCurrency, targetCurrency);

        // 2. 保存询价历史到审计表
        for (BankQuote quote : quotes) {
            QuoteInquiry inquiry = new QuoteInquiry();
            inquiry.setInquiryId(inquiryId);
            inquiry.setRequestNo(requestNo);
            inquiry.setBankCode(quote.getBankCode());
            inquiry.setBankName(quote.getBankName());
            inquiry.setBaseCurrency(originalCurrency.getCode());
            inquiry.setQuoteCurrency(targetCurrency.getCode());
            inquiry.setBuyPrice(quote.getBuyPrice());
            inquiry.setSellPrice(quote.getSellPrice());
            inquiry.setMidPrice(quote.getMidPrice());
            inquiry.setSuccess(quote.isSuccess());
            inquiry.setErrorMessage(quote.getErrorMessage());
            inquiry.setResponseTimeMs(quote.getResponseTimeMs());
            inquiry.setQuoteTime(quote.getQuoteTime() != null ? quote.getQuoteTime() : LocalDateTime.now());
            inquiryRepository.save(inquiry);
        }
        log.info("Saved {} quote inquiry records for inquiryId={}", quotes.size(), inquiryId);

        // 3. 从成功的报价中选择最优买入价
        List<BankQuote> successQuotes = quotes.stream()
                .filter(BankQuote::isSuccess)
                .toList();

        if (successQuotes.isEmpty()) {
            String reason = "所有银行询价均失败，无法进行路由决策";
            log.error("Routing failed: {} - all {} bank quotes failed", inquiryId, quotes.size());
            saveFailedDecision(inquiryId, requestNo, sellerId, originalCurrency,
                             targetCurrency, originalAmount, quotes, reason);
            throw new IllegalStateException(reason);
        }

        // 选择买入价最高的银行（对平台最有利）
        BankQuote bestQuote = successQuotes.stream()
                .max(Comparator.comparing(BankQuote::getBuyPrice))
                .orElseThrow();

        BigDecimal selectedTargetAmount = originalAmount.multiply(bestQuote.getBuyPrice())
                .setScale(2, RoundingMode.HALF_UP);

        // 4. 生成决策原因
        String decisionReason = buildDecisionReason(bestQuote, successQuotes, originalAmount);

        // 5. 保存路由决策到审计表
        String decisionId = generateDecisionId();
        RoutingDecision decision = new RoutingDecision();
        decision.setDecisionId(decisionId);
        decision.setInquiryId(inquiryId);
        decision.setRequestNo(requestNo);
        decision.setSellerId(sellerId);
        decision.setOriginalCurrency(originalCurrency.getCode());
        decision.setTargetCurrency(targetCurrency.getCode());
        decision.setOriginalAmount(originalAmount);
        decision.setSelectedBankCode(bestQuote.getBankCode());
        decision.setSelectedBankName(bestQuote.getBankName());
        decision.setSelectedBuyPrice(bestQuote.getBuyPrice());
        decision.setSelectedTargetAmount(selectedTargetAmount);
        decision.setTotalQuotedBanks(quotes.size());
        decision.setSuccessQuotedBanks(successQuotes.size());
        decision.setDecisionReason(decisionReason);
        decision.setAllQuotesSummary(buildQuotesSummary(quotes));
        decisionRepository.save(decision);
        log.info("Saved routing decision: decisionId={}, selected={}, buyPrice={}, targetAmount={}",
                decisionId, bestQuote.getBankCode(), bestQuote.getBuyPrice(), selectedTargetAmount);

        // 6. 组装返回结果
        MultiBankQuoteResultDTO result = new MultiBankQuoteResultDTO();
        result.setInquiryId(inquiryId);
        result.setRequestNo(requestNo);
        result.setSellerId(sellerId);
        result.setOriginalCurrency(originalCurrency);
        result.setTargetCurrency(targetCurrency);
        result.setOriginalAmount(originalAmount);
        result.setQuotes(convertToDTOs(quotes));
        result.setSelectedBankCode(bestQuote.getBankCode());
        result.setSelectedBankName(bestQuote.getBankName());
        result.setSelectedBuyPrice(bestQuote.getBuyPrice());
        result.setSelectedTargetAmount(selectedTargetAmount);
        result.setDecisionReason(decisionReason);

        return result;
    }

    private String buildDecisionReason(BankQuote best, List<BankQuote> allSuccess,
                                        BigDecimal originalAmount) {
        StringBuilder sb = new StringBuilder();
        sb.append("智能路由选择[").append(best.getBankName()).append("]，");
        sb.append("买入价=").append(best.getBuyPrice()).append("，");
        sb.append("在").append(allSuccess.size()).append("家成功报价中最高。");
        sb.append("结汇金额 ").append(originalAmount).append(" 可兑换 ");
        BigDecimal target = originalAmount.multiply(best.getBuyPrice()).setScale(2, RoundingMode.HALF_UP);
        sb.append(target).append(" CNY。");

        if (allSuccess.size() > 1) {
            sb.append("对比: ");
            for (BankQuote q : allSuccess) {
                if (q != best) {
                    BigDecimal diff = best.getBuyPrice().subtract(q.getBuyPrice())
                            .multiply(originalAmount).setScale(2, RoundingMode.HALF_UP);
                    sb.append(q.getBankName()).append("(买入价=").append(q.getBuyPrice())
                      .append(",差额=").append(diff).append("CNY) ");
                }
            }
        }
        return sb.toString();
    }

    private String buildQuotesSummary(List<BankQuote> quotes) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < quotes.size(); i++) {
            if (i > 0) sb.append(",");
            BankQuote q = quotes.get(i);
            sb.append("{bank:").append(q.getBankCode());
            if (q.isSuccess()) {
                sb.append(",buy:").append(q.getBuyPrice());
                sb.append(",sell:").append(q.getSellPrice());
                sb.append(",mid:").append(q.getMidPrice());
            } else {
                sb.append(",error:").append(q.getErrorMessage());
            }
            sb.append(",time:").append(q.getResponseTimeMs()).append("ms}");
        }
        sb.append("]");
        return sb.toString();
    }

    private void saveFailedDecision(String inquiryId, String requestNo, String sellerId,
                                     Currency originalCurrency, Currency targetCurrency,
                                     BigDecimal originalAmount, List<BankQuote> quotes,
                                     String reason) {
        RoutingDecision decision = new RoutingDecision();
        decision.setDecisionId(generateDecisionId());
        decision.setInquiryId(inquiryId);
        decision.setRequestNo(requestNo);
        decision.setSellerId(sellerId);
        decision.setOriginalCurrency(originalCurrency.getCode());
        decision.setTargetCurrency(targetCurrency.getCode());
        decision.setOriginalAmount(originalAmount);
        decision.setSelectedBankCode("N/A");
        decision.setSelectedBankName("N/A");
        decision.setTotalQuotedBanks(quotes.size());
        decision.setSuccessQuotedBanks(0);
        decision.setDecisionReason(reason);
        decision.setAllQuotesSummary(buildQuotesSummary(quotes));
        decisionRepository.save(decision);
    }

    private List<BankQuoteDTO> convertToDTOs(List<BankQuote> quotes) {
        List<BankQuoteDTO> dtos = new ArrayList<>();
        for (BankQuote q : quotes) {
            BankQuoteDTO dto = new BankQuoteDTO();
            dto.setBankCode(q.getBankCode());
            dto.setBankName(q.getBankName());
            dto.setBuyPrice(q.getBuyPrice());
            dto.setSellPrice(q.getSellPrice());
            dto.setMidPrice(q.getMidPrice());
            dto.setSuccess(q.isSuccess());
            dto.setErrorMessage(q.getErrorMessage());
            dto.setResponseTimeMs(q.getResponseTimeMs());
            dtos.add(dto);
        }
        return dtos;
    }

    private String generateInquiryId() {
        return "INQ" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String generateDecisionId() {
        return "RTE" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
