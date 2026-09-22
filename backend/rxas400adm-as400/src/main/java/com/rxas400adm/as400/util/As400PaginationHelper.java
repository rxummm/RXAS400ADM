package com.rxas400adm.as400.util;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.response.PageResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * AS400 (DB2 for i) 分页查询辅助工具。
 *
 * <p>统一处理：COUNT 查询 → 总数；OFFSET/FETCH 分页查询 → 当前页数据；
 * mock 模式由调用方自行处理（本工具仅处理 prod 模式下的 DB2 分页）。
 *
 * <p>DB2 for i 分页语法（7.2+）：
 * <pre>
 *   SELECT ... FROM ... WHERE ... ORDER BY ...
 *   OFFSET {offset} ROWS FETCH FIRST {size} ROWS ONLY
 * </pre>
 *
 * <p>本工具基于已有 SQL 动态构造分页查询：
 * <ul>
 *   <li>COUNT：将原 SQL 包装为 {@code SELECT COUNT(*) AS CNT FROM (原SQL) _t}（去掉 FETCH FIRST）</li>
 *   <li>PAGED：将原 SQL 的 FETCH FIRST ? ROWS ONLY 替换为 OFFSET ? ROWS FETCH FIRST ? ROWS ONLY</li>
 * </ul>
 */
public final class As400PaginationHelper {

    private static final Pattern FETCH_FIRST_PATTERN = Pattern.compile(
            "\\bFETCH\\s+FIRST\\s+\\?\\s+ROWS\\s+ONLY\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FETCH_FIRST_NUM_PATTERN = Pattern.compile(
            "\\bFETCH\\s+FIRST\\s+\\d+\\s+ROWS\\s+ONLY\\b", Pattern.CASE_INSENSITIVE);

    private As400PaginationHelper() {
    }

    /**
     * 执行分页查询，返回 PageResult。
     *
     * @param clientProvider AS400 客户端提供者
     * @param dataSql        数据查询 SQL（含 FETCH FIRST ? ROWS ONLY）
     * @param current        页码（1-based）
     * @param size           每页条数
     * @param params         原始查询参数（不含 offset/size）
     * @param rowMapper      行映射函数
     * @return PageResult
     */
    public static <T> PageResult<T> queryPaged(
            AS400ClientProvider clientProvider,
            String dataSql,
            int current,
            int size,
            Object[] params,
            Function<Map<String, Object>, T> rowMapper) {

        long pageNum = PageConstants.clampNum(current);
        long pageSize = PageConstants.clampSize(size);

        // 1. 构造 COUNT SQL：去掉 FETCH FIRST ? ROWS ONLY，包装为 COUNT(*)
        String baseSql = stripFetchFirst(dataSql);
        String countSql = "SELECT COUNT(*) AS CNT FROM (" + baseSql + ") _t";

        List<Map<String, Object>> countRows = clientProvider.current().queryListChecked(countSql, params);
        long total = 0;
        if (!countRows.isEmpty()) {
            Object cnt = countRows.get(0).get("CNT");
            total = cnt instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(cnt));
        }

        if (total == 0) {
            return new PageResult<>(0, List.of());
        }

        // 2. 构造分页 SQL：替换 FETCH FIRST ? ROWS ONLY 为 OFFSET ? ROWS FETCH FIRST ? ROWS ONLY
        String pagedSql = baseSql + " OFFSET ? ROWS FETCH FIRST ? ROWS ONLY";
        long offset = (pageNum - 1) * pageSize;

        List<Object> pagedParams = new ArrayList<>();
        if (params != null) {
            for (Object p : params) {
                pagedParams.add(p);
            }
        }
        pagedParams.add(offset);
        pagedParams.add(pageSize);

        List<Map<String, Object>> rows = clientProvider.current().queryListChecked(pagedSql, pagedParams.toArray());

        List<T> records = rows.stream().map(rowMapper).toList();
        return new PageResult<>(total, records);
    }

    /**
     * 重载版本：参数为 List（方便 Service 层拼接）。
     */
    public static <T> PageResult<T> queryPaged(
            AS400ClientProvider clientProvider,
            String dataSql,
            int current,
            int size,
            List<Object> params,
            Function<Map<String, Object>, T> rowMapper) {
        return queryPaged(clientProvider, dataSql, current, size,
                params != null ? params.toArray() : new Object[0], rowMapper);
    }

    /**
     * 不带行映射器的版本（返回原始 Map 列表）。
     */
    public static PageResult<Map<String, Object>> queryPagedRaw(
            AS400ClientProvider clientProvider,
            String dataSql,
            int current,
            int size,
            Object[] params) {

        long pageNum = PageConstants.clampNum(current);
        long pageSize = PageConstants.clampSize(size);

        String baseSql = stripFetchFirst(dataSql);
        String countSql = "SELECT COUNT(*) AS CNT FROM (" + baseSql + ") _t";

        List<Map<String, Object>> countRows = clientProvider.current().queryListChecked(countSql, params);
        long total = 0;
        if (!countRows.isEmpty()) {
            Object cnt = countRows.get(0).get("CNT");
            total = cnt instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(cnt));
        }

        if (total == 0) {
            return new PageResult<>(0, List.of());
        }

        String pagedSql = baseSql + " OFFSET ? ROWS FETCH FIRST ? ROWS ONLY";
        long offset = (pageNum - 1) * pageSize;

        List<Object> pagedParams = new ArrayList<>();
        if (params != null) {
            for (Object p : params) {
                pagedParams.add(p);
            }
        }
        pagedParams.add(offset);
        pagedParams.add(pageSize);

        List<Map<String, Object>> rows = clientProvider.current().queryListChecked(pagedSql, pagedParams.toArray());
        return new PageResult<>(total, rows);
    }

    /**
     * 去掉 SQL 中的 FETCH FIRST ? ROWS ONLY 或 FETCH FIRST n ROWS ONLY 子句。
     */
    private static String stripFetchFirst(String sql) {
        String result = FETCH_FIRST_PATTERN.matcher(sql).replaceAll("");
        result = FETCH_FIRST_NUM_PATTERN.matcher(result).replaceAll("");
        return result.trim();
    }
}
