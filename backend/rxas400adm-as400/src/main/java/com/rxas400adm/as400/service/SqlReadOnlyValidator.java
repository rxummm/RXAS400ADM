package com.rxas400adm.as400.service;

import com.rxas400adm.common.exception.BusinessException;
import com.rxas400adm.common.exception.ErrorCode;

import java.util.regex.Pattern;

/**
 * SQL 只读校验器：仅允许执行单条只读 SELECT/WITH 查询。
 *
 * 约束（B-16/B-17 加固）：
 * 1. 必须是单条语句——出现分号（多语句注入）直接拒绝；
 * 2. 语句必须以 SELECT 或 WITH 开头（含注释剥离后判断）；
 * 3. 拒绝写语句关键字：INSERT/UPDATE/DELETE/DROP/ALTER/CREATE/TRUNCATE/GRANT/REVOKE/CALL/EXEC 等；
 * 4. 拒绝损坏只读语义的 SELECT 变体：SELECT ... INTO / SELECT ... FOR UPDATE / LOCK IN SHARE MODE；
 * 5. 限制长度（防超长 SQL 拖垮 DB/客户端）。
 */
public final class SqlReadOnlyValidator {

    private static final int MAX_SQL_LEN = 4096;

    private static final Pattern WRITE_CLAUSE = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|GRANT|REVOKE|CALL|EXEC(?:UTE)?|MERGE|REPLACE|RENAME)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern READONLY_VIOLATIONS = Pattern.compile(
            "\\b(FOR\\s+UPDATE|INTO|LOCK\\s+IN\\s+SHARE\\s+MODE|FREE\\s+LOCK)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * QSYS2/SYSTOOLS 副作用表函数黑名单（H1）：这些"函数"以 SELECT 形式调用却有写副作用，
     * 不在写关键字黑名单内，会绕过只读校验。QCMDEXC 可执行任意 CL，IFS_* 可写/删 IFS 对象。
     * 只读表函数（ACTIVE_JOB_INFO / JOB_LOG_INFO / SYSTEM_STATUS_INFO / SYSCOLUMNS 等）不在其中。
     */
    private static final Pattern SIDE_EFFECT_FUNCTION = Pattern.compile(
            "\\b(QSYS2|SYSTOOLS|SYSIBMADM)\\.(QCMDEXC|QCMDEXEC|QSYSTEMS|"
                    + "IFS_WRITE|IFS_APPEND|IFS_CREATE_DIRECTORY|IFS_MKDIR|IFS_DELETE|IFS_RENAME|IFS_COPY)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * 无 schema 前缀的副作用函数黑名单：因 DataSource 已设 {@code setLibraries("QSYS2")}，
     * 未限定名的 {@code IFS_WRITE(...)} 等会解析到 QSYS2，同样具备写/执行副作用，必须一并拦截，
     * 否则可绕过上面的带前缀黑名单（B2）。
     */
    private static final Pattern SIDE_EFFECT_FUNCTION_BARE = Pattern.compile(
            "\\b(QCMDEXC|QCMDEXEC|QSYSTEMS|IFS_WRITE|IFS_APPEND|IFS_CREATE_DIRECTORY|IFS_MKDIR|IFS_DELETE|IFS_RENAME|IFS_COPY)\\b",
            Pattern.CASE_INSENSITIVE);

    private SqlReadOnlyValidator() {
    }

    /**
     * 校验 SQL 是否满足只读安全要求，不满足时抛 {@link BusinessException}(SQL_READONLY_REQUIRED)。
     */
    public static void assertReadOnly(String sql) {
        String trimmed = sql == null ? "" : stripSqlComments(sql.trim());
        if (trimmed.isEmpty()) {
            throw new BusinessException(ErrorCode.SQL_READONLY_REQUIRED, "SQL 不能为空");
        }
        if (trimmed.length() > MAX_SQL_LEN) {
            throw new BusinessException(ErrorCode.SQL_READONLY_REQUIRED, "SQL 长度不能超过 " + MAX_SQL_LEN + " 字符");
        }
        if (trimmed.contains(";")) {
            throw new BusinessException(ErrorCode.SQL_READONLY_REQUIRED, "仅允许单条 SQL，禁止多语句/分号");
        }
        String upper = trimmed.toUpperCase();
        if (!upper.startsWith("SELECT") && !upper.startsWith("WITH")) {
            throw new BusinessException(ErrorCode.SQL_READONLY_REQUIRED, "仅允许只读 SELECT/WITH 查询");
        }
        if (WRITE_CLAUSE.matcher(trimmed).find() || READONLY_VIOLATIONS.matcher(trimmed).find()
                || SIDE_EFFECT_FUNCTION.matcher(trimmed).find()
                || SIDE_EFFECT_FUNCTION_BARE.matcher(trimmed).find()) {
            throw new BusinessException(ErrorCode.SQL_READONLY_REQUIRED, "SQL 包含写入/破坏只读语义的操作，已拒绝执行");
        }
    }

    /** 判断是否只读（供 JobScheduleService 等复用），内部校验失败返回 false。 */
    public static boolean isReadOnly(String sql) {
        try {
            assertReadOnly(sql);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    private static String stripSqlComments(String sql) {
        String result = sql.replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("--[^\\r\\n]*", " ");
        return result.trim();
    }
}