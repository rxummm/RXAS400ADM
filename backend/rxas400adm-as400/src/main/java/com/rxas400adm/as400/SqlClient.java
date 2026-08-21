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
     * 无真实环境时返回空列表或仿真数据，不允许抛异常。
     */
    List<Map<String, Object>> queryList(String sql);

    /**
     * 参数化查询（P1-6）：占位符 ? 绑定参数，PreparedStatement 执行，消除手工 sq() 转义面。
     * 无参数时等价于 {@link #queryList(String)}；无真实环境时返回空列表/仿真数据，不允许抛异常。
     */
    List<Map<String, Object>> queryList(String sql, Object... params);

    /**
     * H3：查询失败即抛异常的变体（默认等价于 {@link #queryList(String, Object...)}）。
     * 交互式路径（SQL 控制台 / SQL 型调度）用它，避免"失败被吞成 0 行"误导；
     * 后台采集等优雅降级场景继续用 {@link #queryList(String, Object...)}。
     */
    default List<Map<String, Object>> queryListChecked(String sql, Object... params) {
        return queryList(sql, params);
    }
}
