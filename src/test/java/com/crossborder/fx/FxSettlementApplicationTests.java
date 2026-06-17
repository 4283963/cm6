package com.crossborder.fx;

import com.crossborder.fx.common.Currency;
import com.crossborder.fx.dto.SettlementRequestDTO;
import com.crossborder.fx.dto.SettlementResponseDTO;
import com.crossborder.fx.service.SettlementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FxSettlementApplicationTests {

    @Autowired
    private SettlementService settlementService;

    @Test
    void contextLoads() {
    }

    @Test
    void testSettlementRequest() {
        SettlementRequestDTO request = new SettlementRequestDTO();
        request.setSellerId("SELLER001");
        request.setOriginalCurrency(Currency.USD);
        request.setOriginalAmount(new BigDecimal("10000"));

        SettlementResponseDTO response = settlementService.createSettlementRequest(request);

        assertNotNull(response);
        assertNotNull(response.getRequestNo());
        assertEquals("SELLER001", response.getSellerId());
        assertTrue(response.getRiskCheckPassed());
        assertNotNull(response.getTargetAmount());
        assertTrue(response.getTargetAmount().compareTo(BigDecimal.ZERO) > 0);
    }
}
