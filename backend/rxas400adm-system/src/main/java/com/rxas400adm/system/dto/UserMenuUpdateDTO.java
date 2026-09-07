package com.rxas400adm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户菜单授权更新（B6：替换弱类型 Map，限制集合规模）。
 */
@Data
public class UserMenuUpdateDTO {

    @Size(max = 500, message = "menu count must not exceed 500")
    @Schema(description = "菜单ID列表", example = "[1, 2, 3]")
    private List<Long> menuIds;
}