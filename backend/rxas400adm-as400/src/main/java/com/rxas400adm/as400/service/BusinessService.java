package com.rxas400adm.as400.service;

import com.rxas400adm.as400.AS400ClientProvider;
import com.rxas400adm.common.constants.As400Identifiers;
import com.rxas400adm.common.constants.PageConstants;
import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 通用业务数据查询（借鉴旧项目 biz/order·sku·price + schemaCrud，通用化改造）：
 * 任意 库.表 → 字段定义（QSYS2.SYSCOLUMNS 含描述）+ 分页数据 + 关键词模糊查询。
 * 表名/库名严格白名单校验（防 SQL 注入），数据源按 X-AS400-Server 头路由当前服务器。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessService implements IBusinessService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_TABLE_RESULTS = 500;

    /** IBM i 系统对象名：A-Z 0-9 _ $ # @（已按大写归一） */
    /** S6：收敛至共享常量（原四处独立复制正则） */
    private static final Pattern IDENTIFIER = As400Identifiers.IDENTIFIER;

    private final AS400ClientProvider clientProvider;

    /** 库内文件/表清单（QSYS2.SYSTABLES） */
    public List<Map<String, Object>> tables(String library, String keyword) {
        StringBuilder sql = new StringBuilder(
                "SELECT TABLE_SCHEMA, TABLE_NAME, TABLE_TEXT FROM QSYS2.SYSTABLES WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (library != null && !library.isBlank()) {
            sql.append(" AND TABLE_SCHEMA = ?");
            params.add(library.trim().toUpperCase());
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND TABLE_NAME LIKE ?");
            params.add("%" + keyword.trim().toUpperCase() + "%");
        }
        sql.append(" ORDER BY TABLE_NAME FETCH FIRST ").append(MAX_TABLE_RESULTS).append(" ROWS ONLY");
        return clientProvider.current().queryList(sql.toString(), params.toArray());
    }

    /** 文件字段定义（QSYS2.SYSCOLUMNS，含描述 COLUMN_TEXT） */
    public List<Map<String, Object>> columns(String library, String file) {
        String lib = requireIdentifier(library, "库");
        String tbl = requireIdentifier(file, "文件");
        String sql = "SELECT ORDINAL_POSITION, COLUMN_NAME, DATA_TYPE, LENGTH, SCALE, IS_NULLABLE, "
                + "COLUMN_TEXT, COLUMN_DEFAULT FROM QSYS2.SYSCOLUMNS "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? "
                + "ORDER BY ORDINAL_POSITION";
        return clientProvider.current().queryList(sql, lib, tbl);
    }

    /** 业务数据分页浏览：关键词对字符型字段 LIKE 模糊匹配 */
    public Map<String, Object> data(String library, String file, String keyword, int page, int size) {
        String lib = requireIdentifier(library, "库");
        String tbl = requireIdentifier(file, "文件");
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));

        List<Map<String, Object>> columns = columns(lib, tbl);
        if (columns.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "文件不存在或不可达：" + lib + "." + tbl);
        }
        List<String> charColumns = new ArrayList<>();
        for (Map<String, Object> col : columns) {
            String type = String.valueOf(col.getOrDefault("DATA_TYPE", "")).toUpperCase();
            if (type.startsWith("CHAR") || type.startsWith("VAR")) {
                charColumns.add(String.valueOf(col.get("COLUMN_NAME")));
            }
        }

        List<Object> params = new ArrayList<>();
        String where = buildKeywordWhere(charColumns, keyword, params);
        String countSql = "SELECT COUNT(*) AS CNT FROM " + lib + "." + tbl + where;
        long total = 0;
        List<Map<String, Object>> countRows = clientProvider.current().queryList(countSql, params.toArray());
        if (!countRows.isEmpty()) {
            Object cnt = countRows.get(0).get("CNT");
            total = cnt instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(cnt));
        }

        // B3：page 上限钳制——防 (page-1)*size 溢出为负拼出 OFFSET -20 ROWS（DB2 SQL 错误）
        safePage = Math.min(safePage, PageConstants.MAX_PAGE_NUM);
        long offset = (long) (safePage - 1) * safeSize;
        String dataSql = "SELECT * FROM " + lib + "." + tbl + where
                + " ORDER BY 1 OFFSET " + offset + " ROWS FETCH NEXT " + safeSize + " ROWS ONLY";
        List<Map<String, Object>> rows = clientProvider.current().queryList(dataSql, params.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    private String buildKeywordWhere(List<String> charColumns, String keyword, List<Object> params) {
        if (keyword == null || keyword.isBlank() || charColumns.isEmpty()) {
            return "";
        }
        String pattern = "%" + keyword.trim().toUpperCase() + "%";
        StringBuilder where = new StringBuilder(" WHERE (");
        for (int i = 0; i < charColumns.size(); i++) {
            if (i > 0) {
                where.append(" OR ");
            }
            where.append(charColumns.get(i)).append(" LIKE ?");
            params.add(pattern);
        }
        return where.append(")").toString();
    }

    private String requireIdentifier(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "不能为空");
        }
        String upper = value.trim().toUpperCase();
        if (!IDENTIFIER.matcher(upper).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    label + "名不合法（仅允许 A-Z 0-9 _ $ # @）：" + value);
        }
        return upper;
    }
}
