package com.rxas400adm.security.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private String token;
    /** P2: Refresh token（仅在登录/刷新时返回） */
    private String refreshToken;
    /** Access token 过期时间（毫秒），供前端计算主动刷新时机 */
    private Long expireMs;
    private String username;
    private List<String> permissions;
}
