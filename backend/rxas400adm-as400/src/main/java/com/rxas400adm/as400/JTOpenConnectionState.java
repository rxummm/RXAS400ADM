package com.rxas400adm.as400;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JDBCDataSource;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * JTOpen 委托实现类共享的连接状态（host/user/password + AS400/DataSource 复用）。
 * 原 JTOpenAS400Client 的 private 字段与方法集中于此，委托实现类通过构造函数注入。
 */
@Slf4j
class JTOpenConnectionState {

    static final Pattern IDENTIFIER = Pattern.compile("^[A-Z0-9_$#@]+$");

    final String host;
    final String user;
    final String password;

    private volatile AS400 shared;
    final Object connectLock = new Object();

    private volatile AS400JDBCDataSource dataSource;
    final Object dsLock = new Object();

    JTOpenConnectionState(String host, String user, String password) {
        this.host = host;
        this.user = user;
        this.password = password;
    }

    String name() {
        return "JT400(" + host + ")";
    }

    AS400 connect() {
        if (host == null || host.isBlank()) {
            throw new BusinessException(ErrorCode.AS400_HOST_NOT_CONFIGURED, "未配置 IBM i 主机地址");
        }
        AS400 cached = shared;
        if (cached != null && cached.isConnected(AS400.COMMAND)) {
            return cached;
        }
        synchronized (connectLock) {
            cached = shared;
            if (cached != null && cached.isConnected(AS400.COMMAND)) {
                return cached;
            }
            AS400 fresh = new AS400(host, user, password);
            try {
                // B3：关闭 GUI 探测，避免 IBM i 不可达时多余探测开销
                fresh.setGuiAvailable(false);
                fresh.connectService(AS400.COMMAND);
                fresh.validateSignon();
                shared = fresh;
                return fresh;
            } catch (Exception e) {
                try {
                    fresh.disconnectAllServices();
                } catch (Exception ignored) {
                }
                shared = null;
                throw new BusinessException(ErrorCode.AS400_CONNECTION_FAILED,
                        "IBM i 连接失败: " + redact(e.getMessage()));
            }
        }
    }

    void invalidate(AS400 system) {
        if (system == shared) {
            shared = null;
        }
        if (system != null) {
            try {
                system.disconnectAllServices();
            } catch (Exception ignored) {
            }
        }
    }

    void disconnect() {
        synchronized (connectLock) {
            if (shared != null) {
                try {
                    shared.disconnectAllServices();
                } catch (Exception ignored) {
                }
                shared = null;
            }
        }
        synchronized (dsLock) {
            dataSource = null;
        }
    }

    AS400JDBCDataSource dataSource() {
        AS400JDBCDataSource ds = dataSource;
        if (ds == null) {
            synchronized (dsLock) {
                ds = dataSource;
                if (ds == null) {
                    ds = new AS400JDBCDataSource();
                    ds.setServerName(host);
                    ds.setUser(user);
                    ds.setPassword(password);
                    ds.setNaming("system");
                    ds.setLibraries("QSYS2");
                    ds.setAutoCommit(true);
                    try {
                        ds.setLoginTimeout(15);
                    } catch (SQLException e) {
                        log.warn("设置 JT400 登录超时失败(host={}): {}", host, redact(e.getMessage()));
                    }
                    dataSource = ds;
                }
            }
        }
        return dataSource;
    }

    String redact(String message) {
        if (message == null || password == null || password.isBlank()) {
            return message;
        }
        return message.replace(password, "***");
    }

    static String requireIdentifier(String value, String label) {
        String v = value == null ? "" : value.trim().toUpperCase();
        if (!IDENTIFIER.matcher(v).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    label + " 只能是字母/数字/下划线/$/#/@ 等合法标识符");
        }
        return v;
    }
}