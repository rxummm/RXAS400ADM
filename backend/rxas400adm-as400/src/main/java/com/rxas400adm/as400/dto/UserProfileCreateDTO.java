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

    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 10, message = "用户名长度1-10个字符")
    @Schema(description = "用户名", example = "TESTUSER")
    private String userName;

    @NotBlank(message = "初始密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度6-128个字符")
    @Schema(description = "初始密码", example = "P@ssw0rd")
    private String password;

    @Size(max = 50, message = "描述最长50个字符")
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

    @Size(max = 200, message = "邮箱最长200个字符")
    @Schema(description = "收件人邮箱", example = "user@example.com")
    private String recipientEmail;
}
