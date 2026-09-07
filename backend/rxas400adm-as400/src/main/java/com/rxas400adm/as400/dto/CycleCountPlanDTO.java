package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 循环盘点计划创建/更新 DTO。
 */
public class CycleCountPlanDTO {

    @NotBlank(message = "item number is required")
    @Size(max = 15, message = "max length is 15")
    private String item;

    @Size(max = 50, message = "max length is 50")
    private String itemDesc;

    @NotBlank(message = "warehouse code is required")
    @Size(max = 4, message = "max length is 4")
    private String warehouse;

    @NotNull(message = "planned date is required")
    private LocalDate plannedDate;

    @Size(max = 1, message = "max length is 1")
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
