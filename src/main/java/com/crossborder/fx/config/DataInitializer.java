package com.crossborder.fx.config;

import com.crossborder.fx.common.Currency;
import com.crossborder.fx.entity.Seller;
import com.crossborder.fx.repository.SellerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SellerRepository sellerRepository;

    public DataInitializer(SellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    @Override
    public void run(String... args) {
        if (sellerRepository.count() == 0) {
            log.info("Initializing seller data...");

            createSeller("SELLER001", "深圳华强北电子科技有限公司", Currency.CNY, new BigDecimal("5000000"));
            createSeller("SELLER002", "杭州速卖通网络科技有限公司", Currency.CNY, new BigDecimal("3000000"));
            createSeller("SELLER003", "广州跨境贸易有限公司", Currency.CNY, new BigDecimal("8000000"));
            createSeller("SELLER004", "义乌小商品出口有限公司", Currency.CNY, new BigDecimal("2000000"));
            createSeller("SELLER005", "上海时尚服饰贸易有限公司", Currency.CNY, new BigDecimal("10000000"));

            log.info("Seller data initialized successfully");
        }
    }

    private void createSeller(String sellerId, String name, Currency settlementCurrency, BigDecimal dailyLimit) {
        Seller seller = new Seller();
        seller.setSellerId(sellerId);
        seller.setName(name);
        seller.setSettlementCurrency(settlementCurrency);
        seller.setDailySettlementLimit(dailyLimit);
        seller.setIsActive(true);
        sellerRepository.save(seller);
        log.info("Created seller: {} - {}", sellerId, name);
    }
}
