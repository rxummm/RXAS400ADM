package com.rxas400adm.system.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户菜单授权更新（B6：替换弱类型 Map，限制集合规模）。
 */
@Data
public class UserMenuUpdateDTO {

    @Size(max = 500, message = "菜单数量不能超过 500")
    private List<Long> menuIds;
}
