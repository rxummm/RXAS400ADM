package com.rxas400adm.common.exception;

/**
 * 集中式错误码（借鉴旧项目 ErrorCode 设计，按模块分段）。
 * 0 为成功（符合设计文档统一返回规范）。
 */
public enum ErrorCode {

    // ---------- 通用 ----------
    SUCCESS(0, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统繁忙"),

    // ---------- 用户 / 角色 (10000+) ----------
    USER_NOT_FOUND(10001, "用户不存在"),
    USERNAME_EXISTS(10002, "用户名已存在"),
    LOGIN_FAILED(10003, "登录失败"),
    LOGIN_LOCKED(10004, "账号已锁定"),
    LOGIN_TOO_MANY(10005, "登录尝试过于频繁"),

    // ---------- AS400 实例 (20000+) ----------
    AS400_SERVER_NOT_FOUND(20001, "AS400 服务器配置不存在"),
    AS400_SERVER_DISABLED(20002, "AS400 服务器已禁用"),
    AS400_CONNECTION_FAILED(20003, "AS400 服务器连接失败"),
    AS400_COMMAND_FAILED(20004, "CL 命令执行失败"),
    AS400_SERVER_REQUIRED(20005, "当前请求未指定 AS400 Server"),
    AS400_HOST_NOT_CONFIGURED(20006, "未配置 IBM i 服务器地址"),
    AS400_SQL_FAILED(20007, "SQL 查询执行失败"),

    // ---------- 发布 (30000+) ----------
    DEPLOY_NOT_FOUND(30001, "发布不存在"),
    DEPLOY_STATUS_INVALID(30002, "当前发布状态不允许该操作"),
    APPROVAL_NOT_FOUND(30003, "没有待审批的记录"),
    DEPLOY_ROLLBACK_FAILED(30004, "发布失败，且回滚执行失败"),
    DEPLOY_CONCURRENT(30005, "该发布已在执行中，请勿重复操作"),

    // ---------- 监控 / 告警 (40000+) ----------
    MONITOR_COLLECT_FAILED(40001, "监控指标采集失败"),
    ALERT_RULE_INVALID(40002, "告警规则不合法"),
    ALERT_RULE_NOT_FOUND(40003, "告警规则不存在"),
    ALERT_EVENT_NOT_FOUND(40004, "告警事件不存在"),

    // ---------- 系统 / 管理 (50000+) ----------
    SYSTEM_USER_OPERATION(50001, "用户操作失败"),
    SYSTEM_NOTICE_NOT_FOUND(50002, "公告不存在"),
    SYSTEM_REQUEST_NOT_FOUND(50003, "权限申请不存在"),
    SYSTEM_SCHEDULER_LOCKED(50004, "调度任务已被其他节点占用"),

    // ---------- 报表 / SQL 查询 (60000+) ----------
    REPORT_GENERATE_FAILED(60001, "报表生成失败"),
    SQL_READONLY_REQUIRED(60002, "仅允许执行单条只读 SELECT 查询"),

    // ---------- 编译 (70000+) ----------
    COMPILE_FAILED(70001, "编译失败"),
    COMPILE_TARGET_REQUIRED(70002, "请指定编译目标"),

    // ---------- 源文件 (80000+) ----------
    SOURCE_NOT_FOUND(80001, "源文件不存在"),
    SOURCE_READ_FAILED(80002, "源文件读取失败"),
    FILE_PATH_INVALID(80003, "非法文件路径"),

    // ---------- 作业 / 子系统 (90000+) ----------
    JOB_NOT_FOUND(90001, "作业不存在"),
    JOB_INVALID_STATUS(90002, "非法作业状态"),
    NAME_REQUIRED(90003, "名称不能为空"),

    // ---------- 角色 (110000+) ----------
    ROLE_NOT_FOUND(110001, "角色不存在"),
    ROLE_CODE_EXISTS(110002, "角色编码已存在"),
    ROLE_ADMIN_PROTECTED(110003, "内置 ADMIN 角色不可修改或删除"),

    // ---------- 密码 / 编译 (120000+) ----------
    PASSWORD_POLICY_VIOLATION(120001, "密码不符合安全策略"),
    COMPILE_UNSUPPORTED(120002, "不支持的编译类型");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
