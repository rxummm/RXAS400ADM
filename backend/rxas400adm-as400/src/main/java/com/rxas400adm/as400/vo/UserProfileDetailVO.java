package com.rxas400adm.as400.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * AS400用户Profile详情视图
 */
@Schema(description = "AS400用户Profile详情")
public record UserProfileDetailVO(
        @Schema(description = "用户名") String userName,
        @Schema(description = "状态") String status,
        @Schema(description = "组Profile") String groupProfile,
        @Schema(description = "描述") String description,
        @Schema(description = "初始菜单") String initialMenu,
        @Schema(description = "特殊权限列表") List<String> specialAuthorities,
        @Schema(description = "最后使用日期") String lastUsedDate,
        @Schema(description = "密码过期日期") String passwordExpireDate) {
}
