package com.rxas400adm.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * JWT 吊销名单（rx_token_blacklist，P2-1）：
 * 登出时按 jti 登记，token 有效期内拒绝认证；过期记录由 TokenBlacklistService 定时清理。
 */
@Data
@TableName("rx_token_blacklist")
public class TokenBlacklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** JWT jti（UUID，吊销唯一标识） */
    private String jti;

    /** 所属用户 */
    private String username;

    /** token 到期时间（超过即可物理删除） */
    private LocalDateTime expireTime;

    /** 吊销时间 */
    private LocalDateTime createdTime;
}
