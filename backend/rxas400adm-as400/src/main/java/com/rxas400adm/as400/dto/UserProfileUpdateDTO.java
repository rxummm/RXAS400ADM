package com.rxas400adm.as400.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * AS400用户Profile更新入参
 */
@Data
@Schema(description = "AS400用户Profile更新请求")
public class UserProfileUpdateDTO {

    @Size(max = 50, message = "max length is 50 characters")
    @Schema(description = "用户描述", example = "测试用户")
    private String description;

    @Schema(description = "所属组Profile", example = "*NONE")
    private String groupProfile;

    @Schema(description = "用户状态", example = "*ENABLED")
    private String status;

    @Schema(description = "初始菜单", example = "*SIGNOFF")
    private String initialMenu;

    @Schema(description = "特殊权限列表", example = "[\"*ALLOBJ\", \"*SAVRST\"]")
    private List<String> specialAuthorities;

    @Size(min = 6, max = 128, message = "password length must be 6-128 characters")
    @Schema(description = "新密码（不为空则修改密码）", example = "P@ssw0rd")
    private String newPassword;
}
