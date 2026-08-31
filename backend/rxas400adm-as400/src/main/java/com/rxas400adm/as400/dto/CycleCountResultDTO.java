package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 循环盘点结果录入 DTO。
 */
public class CycleCountResultDTO {

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @NotNull(message = "实盘数量不能为空")
    private int countedQty;

    @Size(max = 100, message = "差异原因最长 100 位")
    private String reason;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public int getCountedQty() { return countedQty; }
    public void setCountedQty(int countedQty) { this.countedQty = countedQty; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
