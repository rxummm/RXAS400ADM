package com.rxas400adm.as400;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JDBCDriver;
import com.ibm.as400.access.AS400JDBCDataSource;
import com.rxas400adm.common.constants.As400Identifiers;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * JTOpen 委托实现类共享的连接状态（host/user/password + AS400/DataSource 复用）。
 * 原 JTOpenAS400Client 的 private 字段与方法集中于此，委托实现类通过构造函数注入。
 */
@Slf4j
class JTOpenConnectionState {

    /** S6：收敛至共享常量（原四处独立复制正则） */
    static final Pattern IDENTIFIER = As400Identifiers.IDENTIFIER;

    final String host;
    final String user;
    final String password;

    private volatile AS400 shared;
    final Object connectLock = new Object();

    private volatile AS400JDBCDataSource dataSource;
    final Object dsLock = new Object();

    /**
     * P1：连接池参数默认值（对应约定 rxas400.as400.pool.max-size 等）。
     * 本类由 new JTOpenConnectionState(host,user,password) 构造（非 Spring bean，无注入通道，
     * 上游构造链在 JTOpenAS400Client/AS400ClientProviderImpl），暂以常量承载默认值。
     */
    private static final int POOL_MAX_SIZE = 8;
    private static final int POOL_MIN_IDLE = 2;
    private static final long POOL_CONNECTION_TIMEOUT_MS = 30_000L;
    private static final long POOL_MAX_LIFETIME_MS = 25 * 60 * 1000L;

    /** P1：按实例惰性创建的 JDBC 连接池——替代「每次 getConnection 物理新建连接（TCP+signon 握手）」 */
    private volatile HikariDataSource pooledDataSource;

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

    /** C12：与 connect() 同锁——避免「A 走完 isConnected 快路径、B 同时 disconnect」的窗口竞争 */
    void invalidate(AS400 system) {
        synchronized (connectLock) {
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
        // P1：连接失效场景（宿主可能不可达）同步关池，下次取连接时惰性重建。
        // 注：关池在 dsLock 内而非 connectLock——建池也走 dsLock，同锁才能防「刚建好即被关」竞态
        synchronized (dsLock) {
            closePoolLocked();
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
            // P1：断开时同步关闭连接池，下次取连接时惰性重建
            closePoolLocked();
            dataSource = null;
        }
    }

    /**
     * P1：取池化 JDBC 连接（JTOpenSqlClient 使用）。
     * 池随本对象惰性创建；本类为普通 new 出来的对象（非 Spring bean），无 @PreDestroy 钩子——
     * 生命周期由 disconnect()/invalidate() 路径关闭 + 进程退出 JVM 兜底（Hikari housekeeper 为守护线程）。
     */
    Connection pooledConnection() throws SQLException {
        return dataSourcePool().getConnection();
    }

    /** P1：DCL 惰性建池。与 {@link #closePoolLocked()} 同用 dsLock，避免「建池中/刚建好」与关池跨锁竞态 */
    private HikariDataSource dataSourcePool() {
        HikariDataSource pool = pooledDataSource;
        if (pool == null) {
            synchronized (dsLock) {
                pool = pooledDataSource;
                if (pool == null) {
                    pool = buildPooledDataSource();
                    pooledDataSource = pool;
                }
            }
        }
        return pool;
    }

    /** P1：按与 {@link #dataSource()} 等价的参数组装 Hikari 池（naming=system、libraries=QSYS2、autoCommit=true） */
    private HikariDataSource buildPooledDataSource() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:as400://" + host + ";naming=system;libraries=QSYS2");
        // 显式指定驱动类，避免依赖 DriverManager ServiceLoader 扫描
        cfg.setDriverClassName(AS400JDBCDriver.class.getName());
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setAutoCommit(true);
        // 终检【HIGH】：jt400 11.0 驱动的连接实现无 JDBC4 isValid()，Hikari 默认校验路径
        // 会抛 AbstractMethodError 导致 prod 档所有池化 SQL 失败——显式指定测试查询兜底
        cfg.setConnectionTestQuery("SELECT 1 FROM SYSIBM.SYSDUMMY1");
        cfg.setMaximumPoolSize(POOL_MAX_SIZE);
        cfg.setMinimumIdle(POOL_MIN_IDLE);
        cfg.setConnectionTimeout(POOL_CONNECTION_TIMEOUT_MS);
        cfg.setMaxLifetime(POOL_MAX_LIFETIME_MS);
        cfg.setPoolName("jt400-pool-" + host);
        return new HikariDataSource(cfg);
    }

    /** P1：关闭并置空池（调用方必须已持有 dsLock：与建池同锁防竞态） */
    private void closePoolLocked() {
        HikariDataSource pool = pooledDataSource;
        pooledDataSource = null;
        if (pool != null && !pool.isClosed()) {
            try {
                pool.close();
            } catch (Exception e) {
                log.warn("关闭 JT400 JDBC 连接池失败(host={}): {}", host, redact(e.getMessage()));
            }
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


    /** 行取值工具：NULL 安全转字符串（各 JTOpen 委托客户端共用，消除 7 处复制） */
    static String str(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    /** 行取值工具：数字/数字字符串安全转 long，异常归 0（各 JTOpen 委托客户端共用） */
    static long lng(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v != null && !String.valueOf(v).isBlank()) {
            try {
                return Long.parseLong(String.valueOf(v).trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
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