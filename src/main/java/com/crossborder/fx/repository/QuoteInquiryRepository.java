package com.crossborder.fx.repository;

import com.crossborder.fx.entity.QuoteInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteInquiryRepository extends JpaRepository<QuoteInquiry, Long> {

    List<QuoteInquiry> findByInquiryId(String inquiryId);

    List<QuoteInquiry> findByRequestNo(String requestNo);

    List<QuoteInquiry> findByBankCode(String bankCode);
}
