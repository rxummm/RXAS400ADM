package com.rxas400adm.as400.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * AS400用户Profile创建入参
 */
@Data
@Schema(description = "AS400用户Profile创建请求")
public class UserProfileCreateDTO {

    @NotBlank(message = "username is required")
    @Size(min = 1, max = 10, message = "username length must be 1-10 characters")
    @Schema(description = "用户名", example = "TESTUSER")
    private String userName;

    @NotBlank(message = "initial password is required")
    @Size(min = 6, max = 128, message = "password length must be 6-128 characters")
    @Schema(description = "初始密码", example = "P@ssw0rd")
    private String password;

    @Size(max = 50, message = "max length is 50 characters")
    @Schema(description = "用户描述", example = "测试用户")
    private String description;

    @Schema(description = "所属组Profile", example = "*NONE")
    private String groupProfile;

    @Schema(description = "初始菜单", example = "*SIGNOFF")
    private String initialMenu;

    @Schema(description = "特殊权限列表", example = "[\"*ALLOBJ\", \"*SAVRST\"]")
    private List<String> specialAuthorities;

    @Schema(description = "是否发送邮件通知", example = "NO")
    private String emailNotification;

    @Size(max = 200, message = "max length is 200 characters")
    @Schema(description = "收件人邮箱", example = "user@example.com")
    private String recipientEmail;
}
