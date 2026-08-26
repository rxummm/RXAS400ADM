package com.rxas400adm.common.security;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * S4：高危 CL 动词黑名单校验器（执行期兜底）。
 *
 * 背景：CL 命令入口分散（命令脚本 / 操作模板 / 作业调度 / 服务器命令直执行），
 * 创建侧校验无法覆盖历史脏数据与绕过路径，故在四个执行出口统一兜底。
 *
 * 规则：
 * 1. 取命令首个非空、非注释行的首个空白前 token，大写化后与黑名单做前缀匹配；
 * 2. 命中即抛 BusinessException(FORBIDDEN)，消息带命中的动词；
 * 3. 空命令/纯注释不拦截（空值由各入口自身的非空校验负责）。
 *
 * 【第六章·P1】配置来源迁移：内置 17 动词保持硬编码（安全基线随代码版本走），
 * 开关与扩展动词经 {@link ClBlacklistConfigProvider} 从 rx_config 运行时读取
 * （系统配置页维护，实时生效），本类内置 60s TTL 快照缓存降低读取频率；
 * provider 异常时沿用上一次快照（fail-safe：宁可沿用旧黑名单也不放行）。
 */
@Component
public class DangerousClCommandValidator {

    /** 配置快照 TTL（毫秒）：60s 内复用，过期才回调 provider */
    static final long CONFIG_TTL_MS = 60_000L;

    /**
     * 内置高危动词集（均为 IBM i 确定存在的系统级破坏性命令）：
     * 用户资料增删改 / 对象与库删除 / 清文件 / 系统值与网络属性篡改 /
     * 关机断电 / TCP 服务启停 / 消息队列与对象删除 / 属主与授权变更。
     */
    private static final List<String> BUILTIN_VERBS = List.of(
            "CHGUSRPRF", "CRTUSRPRF", "DLTUSRPRF",
            "DLTLIB", "DLTF", "CLRPFM",
            "CHGSYSVAL", "CHGNETA",
            "ENDSYS", "PWRDWNSYS",
            "ENDTCPSVR", "STRTCPSVR",
            "DLTMSGQ", "DLTOBJ", "CHGOBJOWN",
            "GRTOBJAUT", "RVKOBJAUT");

    private final ClBlacklistConfigProvider provider;

    /** 配置快照（volatile 保证跨线程可见）；加载失败沿用旧值 */
    private volatile Snapshot snapshot = new Snapshot(true, List.of());
    private volatile long loadedAtMs;

    /** 不可变配置快照 */
    private record Snapshot(boolean enabled, List<String> extraVerbs) {
    }

    public DangerousClCommandValidator(ClBlacklistConfigProvider provider) {
        this.provider = provider;
    }

    /**
     * 校验命令是否允许执行，命中黑名单即抛 {@link BusinessException}(FORBIDDEN)。
     */
    public void assertAllowed(String command) {
        Snapshot snap = currentSnapshot();
        if (!snap.enabled()) {
            return; // 管理员在系统配置页显式关闭黑名单时不拦截
        }
        String verb = firstVerb(command);
        if (verb.isEmpty()) {
            return; // 空命令/纯注释交给入口各自的非空校验
        }
        for (String banned : BUILTIN_VERBS) {
            if (verb.startsWith(banned)) {
                throw reject(verb);
            }
        }
        for (String banned : snap.extraVerbs()) {
            if (!banned.isEmpty() && verb.startsWith(banned)) {
                throw reject(verb);
            }
        }
    }

    /** 命中黑名单的统一拒绝异常 */
    private BusinessException reject(String verb) {
        return new BusinessException(ErrorCode.FORBIDDEN,
                "CL 命令包含被禁止的高危动词: " + verb);
    }

    /** 60s TTL 快照：过期才回调 provider；provider 抛异常时沿用旧快照（fail-safe） */
    private synchronized Snapshot currentSnapshot() {
        long now = System.currentTimeMillis();
        if (snapshot == null || now - loadedAtMs >= CONFIG_TTL_MS) {
            try {
                List<String> extra = provider.extraVerbs() == null ? List.of() : provider.extraVerbs();
                snapshot = new Snapshot(provider.enabled(), List.copyOf(extra));
                loadedAtMs = now;
            } catch (Exception e) {
                // 保留旧快照；若从未成功加载则保持安全默认（enabled=true、无扩展）
                loadedAtMs = now; // 避免异常场景下每请求重试打库
            }
        }
        return snapshot;
    }

    /**
     * 提取命令首 token：trim → 剥离行内与整行注释（终检 B3：仅按行前缀跳过可被
     * 单行「注释后接真实命令」绕过）→ 取首个非空行的首个空白前 token 并大写。
     */
    private String firstVerb(String command) {
        if (command == null) {
            return "";
        }
        // 先剥离 ... 行内/跨行注释，再逐行取首 token
        String stripped = command.replaceAll("/\\*.*?\\*/", " ");
        for (String rawLine : stripped.trim().split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] tokens = line.split("\\s+");
            return tokens[0].toUpperCase(Locale.ROOT);
        }
        return "";
    }
}
