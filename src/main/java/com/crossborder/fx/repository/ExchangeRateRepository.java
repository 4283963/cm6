package com.crossborder.fx.repository;

import com.crossborder.fx.common.Currency;
import com.crossborder.fx.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    @Query("SELECT r FROM ExchangeRate r WHERE r.baseCurrency = :base AND r.quoteCurrency = :quote ORDER BY r.timestamp DESC")
    List<ExchangeRate> findLatestRate(@Param("base") Currency base, @Param("quote") Currency quote);

    default Optional<ExchangeRate> findLatestByBaseAndQuote(Currency base, Currency quote) {
        List<ExchangeRate> rates = findLatestRate(base, quote);
        return rates.isEmpty() ? Optional.empty() : Optional.of(rates.get(0));
    }
}
