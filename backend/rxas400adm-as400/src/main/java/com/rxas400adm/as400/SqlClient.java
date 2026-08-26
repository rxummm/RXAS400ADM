package com.rxas400adm.as400;

import java.util.List;
import java.util.Map;

/**
 * SQL 域：QSYS2 通用查询（列名随 SQL 变化，保持 Map 返回）。
 */
public interface SqlClient {

    /** 查询 QSYS2 SQL 返回单行（如 SYSTEM_STATUS_INFO） */
    Map<String, Object> querySingle(String sql);

    /**
     * 查询 QSYS2 SQL 返回多行（如 ACTIVE_JOB_INFO / ASP_INFO / SUBSYSTEM_INFO）。
     * E1（2026-08-25）：失败不再「吞错返空集」——JTOpen 实现统一抛 BusinessException，
     * 由调用方决定降级方式；监控侧表现为数据点缺失（gap）而非假 0 值。Mock 实现仍返回仿真数据不抛异常。
     */
    List<Map<String, Object>> queryList(String sql);

    /**
     * 参数化查询（P1-6）：占位符 ? 绑定参数，PreparedStatement 执行，消除手工 sq() 转义面。
     * 无参数时等价于 {@link #queryList(String)}；异常语义同上。
     */
    List<Map<String, Object>> queryList(String sql, Object... params);

    /**
     * H3：查询失败即抛异常的变体（默认等价于 {@link #queryList(String, Object...)}）。
     * 交互式路径（SQL 控制台 / SQL 型调度）用它，避免"失败被吞成 0 行"误导；
     * 后台采集等优雅降级场景继续用 {@link #queryList(String, Object...)} 并自行 catch。
     */
    default List<Map<String, Object>> queryListChecked(String sql, Object... params) {
        return queryList(sql, params);
    }

    /**
     * S7：带服务端行数上限的变体——JDBC setMaxRows 下推截断，大表查询不再全量进堆。
     * maxRows <= 0 时忽略上限。Mock 实现走默认方法忽略上限（仿真数据量极小）。
     * 注：不做成 {@code queryListChecked} 的 int 重载——(String,int) 调用点会在两个
     * varargs 候选间产生二义性，故使用独立方法名。
     */
    default List<Map<String, Object>> queryListCheckedBounded(String sql, int maxRows, Object... params) {
        return queryListChecked(sql, params);
    }
}
