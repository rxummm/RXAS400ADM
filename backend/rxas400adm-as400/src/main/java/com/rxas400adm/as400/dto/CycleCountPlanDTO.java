package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 循环盘点计划创建/更新 DTO。
 */
public class CycleCountPlanDTO {

    @NotBlank(message = "物料号不能为空")
    @Size(max = 15, message = "物料号最长 15 位")
    private String item;

    @Size(max = 50, message = "物料描述最长 50 位")
    private String itemDesc;

    @NotBlank(message = "仓库代码不能为空")
    @Size(max = 4, message = "仓库代码最长 4 位")
    private String warehouse;

    @NotNull(message = "计划日期不能为空")
    private LocalDate plannedDate;

    @Size(max = 1, message = "ABC 分类最长 1 位")
    private String abcClass;

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
    public String getItemDesc() { return itemDesc; }
    public void setItemDesc(String itemDesc) { this.itemDesc = itemDesc; }
    public String getWarehouse() { return warehouse; }
    public void setWarehouse(String warehouse) { this.warehouse = warehouse; }
    public LocalDate getPlannedDate() { return plannedDate; }
    public void setPlannedDate(LocalDate plannedDate) { this.plannedDate = plannedDate; }
    public String getAbcClass() { return abcClass; }
    public void setAbcClass(String abcClass) { this.abcClass = abcClass; }
}
