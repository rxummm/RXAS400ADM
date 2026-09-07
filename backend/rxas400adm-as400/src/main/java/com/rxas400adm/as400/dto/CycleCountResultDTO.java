package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 循环盘点结果录入 DTO。
 */
public class CycleCountResultDTO {

    @NotNull(message = "plan ID is required")
    private Long planId;

    @NotNull(message = "actual count is required")
    private Integer countedQty;

    @Size(max = 100, message = "max length is 100")
    private String reason;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Integer getCountedQty() { return countedQty; }
    public void setCountedQty(Integer countedQty) { this.countedQty = countedQty; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
