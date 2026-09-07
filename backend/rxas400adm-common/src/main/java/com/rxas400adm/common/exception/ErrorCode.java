package com.rxas400adm.common.exception;

/**
 * 集中式错误码（借鉴旧项目 ErrorCode 设计，按模块分段）。
 * 0 为成功（符合设计文档统一返回规范）。
 */
public enum ErrorCode {

    // ---------- Common ----------
    SUCCESS(0, "Operation succeeded"),
    BAD_REQUEST(400, "Invalid request parameter"),
    UNAUTHORIZED(401, "Not logged in or session expired"),
    FORBIDDEN(403, "Access denied"),
    NOT_FOUND(404, "Resource not found"),
    INTERNAL_ERROR(500, "Internal server error"),

    // ---------- User / Role (10000+) ----------
    USER_NOT_FOUND(10001, "User not found"),
    USERNAME_EXISTS(10002, "Username already exists"),
    LOGIN_FAILED(10003, "Login failed"),
    LOGIN_LOCKED(10004, "Account locked"),
    LOGIN_TOO_MANY(10005, "Too many login attempts"),

    // ---------- AS400 Instance (20000+) ----------
    AS400_SERVER_NOT_FOUND(20001, "AS400 server configuration not found"),
    AS400_SERVER_DISABLED(20002, "AS400 server is disabled"),
    AS400_CONNECTION_FAILED(20003, "AS400 server connection failed"),
    AS400_COMMAND_FAILED(20004, "CL command execution failed"),
    AS400_SERVER_REQUIRED(20005, "No AS400 server specified in request"),
    AS400_HOST_NOT_CONFIGURED(20006, "IBM i server address not configured"),
    AS400_SQL_FAILED(20007, "SQL query execution failed"),
    AS400_COMMAND_BUSY(20008, "Command concurrency limit reached, please retry later"),

    // ---------- Deploy (30000+) ----------
    DEPLOY_NOT_FOUND(30001, "Deployment not found"),
    DEPLOY_STATUS_INVALID(30002, "Current deployment status does not allow this operation"),
    APPROVAL_NOT_FOUND(30003, "No pending approval record"),
    DEPLOY_ROLLBACK_FAILED(30004, "Deployment failed and rollback also failed"),
    DEPLOY_CONCURRENT(30005, "Deployment already in progress, please do not submit again"),

    // ---------- Monitor / Alert (40000+) ----------
    MONITOR_COLLECT_FAILED(40001, "Monitor metric collection failed"),
    ALERT_RULE_INVALID(40002, "Invalid alert rule"),
    ALERT_RULE_NOT_FOUND(40003, "Alert rule not found"),
    ALERT_EVENT_NOT_FOUND(40004, "Alert event not found"),

    // ---------- System / Admin (50000+) ----------
    SYSTEM_USER_OPERATION(50001, "User operation failed"),
    SYSTEM_NOTICE_NOT_FOUND(50002, "Notice not found"),
    SYSTEM_REQUEST_NOT_FOUND(50003, "Permission request not found"),
    SYSTEM_SCHEDULER_LOCKED(50004, "Scheduler task occupied by another node"),

    // ---------- Report / SQL (60000+) ----------
    REPORT_GENERATE_FAILED(60001, "Report generation failed"),
    SQL_READONLY_REQUIRED(60002, "Only single read-only SELECT queries are allowed"),

    // ---------- Compile (70000+) ----------
    // Compile feature offline since V56; 70001/70002 removed, do not reuse this range

    // ---------- Source File (80000+) ----------
    SOURCE_NOT_FOUND(80001, "Source file not found"),
    SOURCE_READ_FAILED(80002, "Failed to read source file"),
    FILE_PATH_INVALID(80003, "Invalid file path"),

    // ---------- Job / Subsystem (90000+) ----------
    JOB_NOT_FOUND(90001, "Job not found"),
    JOB_INVALID_STATUS(90002, "Invalid job status"),
    NAME_REQUIRED(90003, "Name is required"),

    // ---------- Role (110000+) ----------
    ROLE_NOT_FOUND(110001, "Role not found"),
    ROLE_CODE_EXISTS(110002, "Role code already exists"),
    ROLE_ADMIN_PROTECTED(110003, "Built-in ADMIN role cannot be modified or deleted"),

    // ---------- Password (120000+) ----------
    PASSWORD_POLICY_VIOLATION(120001, "Password does not meet security policy"),

    // ---------- Email (130000+) ----------
    EMAIL_SMTP_NOT_CONFIGURED(130001, "SMTP server not configured"),
    EMAIL_SEND_FAILED(130002, "Email sending failed"),
    EMAIL_GROUP_NOT_FOUND(130003, "Email group not found"),
    EMAIL_RECIPIENT_EXISTS(130004, "Email address already in group");

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
