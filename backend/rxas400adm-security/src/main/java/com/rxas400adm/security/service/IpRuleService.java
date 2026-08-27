package com.rxas400adm.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.rxas400adm.common.util.EntityUtil;
import com.rxas400adm.common.response.PageResult;
import com.rxas400adm.security.dto.IpRuleCreateDTO;
import com.rxas400adm.security.dto.IpRuleUpdateDTO;
import com.rxas400adm.security.entity.IpRule;
import com.rxas400adm.security.mapper.IpRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录 IP 黑白名单（rx_ip_rule）：
 * - 黑名单（BLACK）：命中即拒绝登录（含平台 + AS400）
 * - 白名单（WHITE）：配置后仅命中白名单的 IP 允许登录；未命中同样拒绝
 * 支持通配 * 与 CIDR 网段。
 */
@Service
@RequiredArgsConstructor
public class IpRuleService implements IIpRuleService {

    private final IpRuleMapper ipRuleMapper;

    /** 登录前置校验：黑名单命中拒绝；白名单存在且未命中拒绝 */
    public void checkIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        List<IpRule> enabled = ipRuleMapper.selectList(new LambdaQueryWrapper<IpRule>()
                .eq(IpRule::getEnabled, 1));
        if (enabled.isEmpty()) {
            return;
        }
        boolean blackHit = enabled.stream()
                .filter(r -> "BLACK".equals(r.getType()))
                .anyMatch(r -> match(r.getIp(), ip));
        if (blackHit) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前 IP 已被加入黑名单，禁止登录");
        }
        List<String> whites = enabled.stream()
                .filter(r -> "WHITE".equals(r.getType()))
                .map(IpRule::getIp)
                .toList();
        if (!whites.isEmpty() && whites.stream().noneMatch(w -> match(w, ip))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前 IP 不在白名单内，禁止登录");
        }
    }

    /**
     * IP 匹配：支持精确、通配 *（如 192.168.1.*）与 CIDR（如 10.0.0.0/8）。
     */
    boolean match(String rule, String ip) {
        if (rule == null || ip == null) {
            return false;
        }
        String r = rule.trim();
        String v = ip.trim();
        if (r.isEmpty() || v.isEmpty()) {
            return false;
        }
        // CIDR
        if (r.contains("/")) {
            return matchCidr(r, v);
        }
        // 通配
        if (r.contains("*")) {
            String regex = r.replace(".", "\\.").replace("*", "\\d+").replaceAll("\\\\\\d\\+", "\\d+");
            return v.matches(regex);
        }
        return r.equals(v);
    }

    private boolean matchCidr(String cidr, String ip) {
        try {
            String[] parts = cidr.split("/");
            long ipLong = ipToLong(ip);
            long netLong = ipToLong(parts[0]);
            int bits = Integer.parseInt(parts[1]);
            int shift = 32 - bits;
            long mask = bits == 0 ? 0 : (0xFFFFFFFFL << shift) & 0xFFFFFFFFL;
            return (ipLong & mask) == (netLong & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) + Integer.parseInt(octets[i]);
        }
        return result;
    }

    // ---------- CRUD ----------

    public PageResult<IpRule> page(int current, int size, String type, String keyword) {
        LambdaQueryWrapper<IpRule> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type)) {
            wrapper.eq(IpRule::getType, type.trim());
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(q -> q.like(IpRule::getIp, kw).or().like(IpRule::getDescription, kw));
        }
        wrapper.orderByAsc(IpRule::getType).orderByAsc(IpRule::getIp);
        Page<IpRule> page = ipRuleMapper.selectPage(
                new Page<>(PageConstants.clampNum(current), PageConstants.clampSize(size)), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }


    public IpRule create(IpRuleCreateDTO dto, String username) {
        if (!StringUtils.hasText(dto.ip()) || !StringUtils.hasText(dto.type())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "IP 与类型必填");
        }
        if (!"BLACK".equals(dto.type()) && !"WHITE".equals(dto.type())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "类型仅支持 BLACK/WHITE");
        }
        long exists = ipRuleMapper.selectCount(new LambdaQueryWrapper<IpRule>()
                .eq(IpRule::getIp, dto.ip().trim())
                .eq(IpRule::getType, dto.type().trim()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "同类型 IP 规则已存在: " + dto.ip());
        }
        IpRule rule = new IpRule();
        rule.setIp(dto.ip().trim());
        rule.setType(dto.type());
        rule.setDescription(dto.description());
        rule.setEnabled(dto.enabled() == null ? 1 : dto.enabled());
        rule.setCreatedBy(username);
        rule.setCreatedTime(LocalDateTime.now());
        rule.setUpdatedTime(LocalDateTime.now());
        ipRuleMapper.insert(rule);
        return rule;
    }


    public IpRule update(Long id, IpRuleUpdateDTO dto) {
        IpRule rule = EntityUtil.require(id, "IP 规则", ipRuleMapper::selectById);
        if (StringUtils.hasText(dto.ip())) {
            rule.setIp(dto.ip().trim());
        }
        if (StringUtils.hasText(dto.type())) {
            rule.setType(dto.type());
        }
        if (dto.description() != null) {
            rule.setDescription(dto.description());
        }
        if (dto.enabled() != null) {
            rule.setEnabled(dto.enabled());
        }
        rule.setUpdatedTime(LocalDateTime.now());
        ipRuleMapper.updateById(rule);
        return rule;
    }

    
    public void delete(Long id) {
        ipRuleMapper.deleteById(EntityUtil.require(id, "IP 规则", ipRuleMapper::selectById).getId());
    }


}