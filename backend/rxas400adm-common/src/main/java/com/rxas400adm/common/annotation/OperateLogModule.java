package com.rxas400adm.common.annotation;

/**
 * 操作日志模块常量（@OperateLog module 值）。
 * 统一使用英文常量，避免中文硬编码。
 */
public final class OperateLogModule {

    private OperateLogModule() {}

    // ===== system 模块 =====
    public static final String USER_MANAGEMENT = "USER_MANAGEMENT";
    public static final String ROLE_MANAGEMENT = "ROLE_MANAGEMENT";
    public static final String MENU_MANAGEMENT = "MENU_MANAGEMENT";
    public static final String PERMISSION_MANAGEMENT = "PERMISSION_MANAGEMENT";
    public static final String PERMISSION_REQUEST = "PERMISSION_REQUEST";
    public static final String CONFIG_MANAGEMENT = "CONFIG_MANAGEMENT";
    public static final String DICT_MANAGEMENT = "DICT_MANAGEMENT";
    public static final String NOTICE_MANAGEMENT = "NOTICE_MANAGEMENT";
    public static final String NOTIFICATION_CENTER = "NOTIFICATION_CENTER";
    public static final String DASHBOARD_WIDGET = "DASHBOARD_WIDGET";
    public static final String FAVORITE_MANAGEMENT = "FAVORITE_MANAGEMENT";
    public static final String CALENDAR_MANAGEMENT = "CALENDAR_MANAGEMENT";
    public static final String REGION_MANAGEMENT = "REGION_MANAGEMENT";
    public static final String SYS_DOC_MANAGEMENT = "SYS_DOC_MANAGEMENT";
    public static final String EMAIL_GROUP = "EMAIL_GROUP";
    public static final String EMAIL_LOG = "EMAIL_LOG";
    public static final String I18N_MANAGEMENT = "I18N_MANAGEMENT";

    // ===== security 模块 =====
    public static final String AUTH = "AUTH";
    public static final String LOGIN_SECURITY = "LOGIN_SECURITY";

    // ===== as400 模块 =====
    public static final String AS400_MANAGEMENT = "AS400_MANAGEMENT";
    public static final String IFS_FILE = "IFS_FILE";
    public static final String DOC_MANAGEMENT = "DOC_MANAGEMENT";
    public static final String DATA_AREA = "DATA_AREA";
    public static final String MESSAGE_FILE = "MESSAGE_FILE";
    public static final String BPCS_SHIPMENT = "BPCS_SHIPMENT";
    public static final String BPCS_INVENTORY = "BPCS_INVENTORY";
    public static final String FREIGHT_COST = "FREIGHT_COST";
    public static final String FREIGHT_RECORD = "FREIGHT_RECORD";
    public static final String ORDER_COLLABORATION = "ORDER_COLLABORATION";
    public static final String COLLABORATION_NOTIFICATION = "COLLABORATION_NOTIFICATION";
    public static final String INVENTORY_SIMULATION = "INVENTORY_SIMULATION";

    // ===== approval 模块 =====
    public static final String APPROVAL = "APPROVAL";

    // ===== monitor 模块 =====
    public static final String ALERT_RULE = "ALERT_RULE";
    public static final String WEBHOOK_MANAGEMENT = "WEBHOOK_MANAGEMENT";
    public static final String WEBHOOK_LOG = "WEBHOOK_LOG";
    public static final String AUDIT_LOG = "AUDIT_LOG";
    public static final String LOGIN_LOG = "LOGIN_LOG";
    public static final String IBMI_SYSTEM = "IBMI_SYSTEM";

    // ===== quality 模块 =====
    public static final String QUALITY = "QUALITY";

    // ===== cost 模块 =====
    public static final String COST = "COST";

    // ===== mrp 模块 =====
    public static final String MRP = "MRP";

    // ===== tpm 模块 =====
    public static final String TPM = "TPM";

    // ===== edi 模块 =====
    public static final String EDI = "EDI";

    // ===== olap 模块 =====
    public static final String OLAP = "OLAP";

    // ===== app 模块 =====
    public static final String CACHE_MANAGEMENT = "CACHE_MANAGEMENT";
    public static final String PLATFORM_TASK = "PLATFORM_TASK";
    public static final String FINANCE_AR = "FINANCE_AR";
    public static final String EMAIL_MANAGEMENT = "EMAIL_MANAGEMENT";
    public static final String REPORT_SCHEDULE = "REPORT_SCHEDULE";
    public static final String REPORT_BUILDER = "REPORT_BUILDER";
    public static final String PURCHASE_ORDER = "PURCHASE_ORDER";

    // ===== as400 模块（补充） =====
    public static final String JOB_SLA = "JOB_SLA";
    public static final String OP_TEMPLATE = "OP_TEMPLATE";
    public static final String JOB_SCHEDULE = "JOB_SCHEDULE";
    public static final String COMMAND_SCRIPT = "COMMAND_SCRIPT";
    public static final String SQL_QUERY = "SQL_QUERY";
    public static final String SUBSYSTEM = "SUBSYSTEM";
    public static final String SYSTEM_VALUE = "SYSTEM_VALUE";
    public static final String USER_PROFILE = "USER_PROFILE";
    public static final String AS400_USER_PROFILE = "AS400_USER_PROFILE";
    public static final String BPCS_ORDER_COPY = "BPCS_ORDER_COPY";

    // ===== operation 模块 =====
    public static final String OPERATION = "OPERATION";
}
