package com.rxas400adm.as400.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * RMA 状态更新 DTO（替代 Controller 层裸 Map）。
 */
@Data
public class RmaStatusUpdateDTO {
    @NotBlank
    private String status;
}
