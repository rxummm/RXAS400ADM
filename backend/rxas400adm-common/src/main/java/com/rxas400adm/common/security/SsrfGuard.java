package com.rxas400adm.common.security;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;

/**
 * L2：SSRF 防护——Webhook 等出站推送 URL 白名单校验。
 * 仅放行 http/https，且目标主机不允许为私网/回环/链路本地/组播/任意地址
 * （防云 metadata 169.254.169.254、localhost、内网穿透等 SSRF）。
 * 主机名按 DNS 全部分解结果校验（任一命中私网即拒绝），防御纵深最佳实践；
 * DNS 重绑定属残余风险，故调用链上 push 前再次校验（校验点在唯一出口 WebhookNotifier）。
 */
public final class SsrfGuard {

    private SsrfGuard() {
    }

    /** 校验 URL 是否安全（不安全时抛 BusinessException，用于配置保存时快速失败）。 */
    public static void assertSafeUrl(String url) {
        if (!isSafeUrl(url)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Invalid webhook URL: only public http/https allowed, internal/loopback/link-local addresses blocked");
        }
    }

    /** 判断 URL 是否安全（供无异常语义的出站发送出口使用）。 */
    public static boolean isSafeUrl(String url) {
        try {
            URI uri = new URI(url == null ? "" : url.trim());
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return false;
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                return false;
            }
            for (InetAddress addr : addresses) {
                if (isBlocked(addr)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            // 无法解析/畸形 URL → 视为不安全（宁可推送失败也不放行未知目标）
            return false;
        }
    }

    /** 判断是否为禁止访问的地址段（私网/回环/链路本地/任意/组播/CGNAT）。 */
    static boolean isBlocked(InetAddress addr) {
        if (addr.isLoopbackAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()
                || addr.isAnyLocalAddress()
                || addr.isMulticastAddress()) {
            return true;
        }
        // IPv4 100.64.0.0/10（运营商 CGNAT，云/内网常见，Java 未内置判定）
        if (addr instanceof Inet4Address) {
            byte[] b = addr.getAddress();
            return (b[0] & 0xFF) == 100 && (b[1] & 0xC0) == 0x40;
        }
        // IPv6 fc00::/7（唯一本地地址 ULA，Java isSiteLocalAddress 不含）
        if (addr instanceof Inet6Address) {
            byte[] b = addr.getAddress();
            return (b[0] & 0xFE) == 0xFC;
        }
        return false;
    }
}