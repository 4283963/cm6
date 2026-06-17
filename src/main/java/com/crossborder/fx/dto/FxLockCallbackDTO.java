package com.crossborder.fx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class FxLockCallbackDTO {

    @NotBlank(message = "幂等令牌不能为空")
    private String idempotentToken;

    @NotBlank(message = "结汇申请单号不能为空")
    private String requestNo;

    @NotBlank(message = "锁汇状态不能为空")
    private String status;

    @Positive(message = "锁汇汇率必须大于0")
    private BigDecimal lockRate;

    @Positive(message = "锁汇金额必须大于0")
    private BigDecimal lockAmount;

    private String lockCurrency;

    private String settlementCurrency;

    private BigDecimal settlementAmount;

    private String callbackTime;

    private String remark;

    public String getIdempotentToken() { return idempotentToken; }
    public void setIdempotentToken(String idempotentToken) { this.idempotentToken = idempotentToken; }

    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getLockRate() { return lockRate; }
    public void setLockRate(BigDecimal lockRate) { this.lockRate = lockRate; }

    public BigDecimal getLockAmount() { return lockAmount; }
    public void setLockAmount(BigDecimal lockAmount) { this.lockAmount = lockAmount; }

    public String getLockCurrency() { return lockCurrency; }
    public void setLockCurrency(String lockCurrency) { this.lockCurrency = lockCurrency; }

    public String getSettlementCurrency() { return settlementCurrency; }
    public void setSettlementCurrency(String settlementCurrency) { this.settlementCurrency = settlementCurrency; }

    public BigDecimal getSettlementAmount() { return settlementAmount; }
    public void setSettlementAmount(BigDecimal settlementAmount) { this.settlementAmount = settlementAmount; }

    public String getCallbackTime() { return callbackTime; }
    public void setCallbackTime(String callbackTime) { this.callbackTime = callbackTime; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
