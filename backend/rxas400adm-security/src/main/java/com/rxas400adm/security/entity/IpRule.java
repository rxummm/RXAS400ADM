package com.rxas400adm.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录 IP 黑白名单（rx_ip_rule）：BLACK 命中直接拒绝登录，
 * WHITE 配置后仅白名单内 IP 允许登录（空=不启用白名单）。
 */
@Data
@TableName("rx_ip_rule")
public class IpRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 支持通配 * 与 CIDR，如 192.168.1.* / 10.0.0.0/8 */
    private String ip;

    /** BLACK / WHITE */
    private String type;

    private String description;

    /** 1=启用 0=停用 */
    private Integer enabled;

    private String createdBy;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}