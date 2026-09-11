package com.rxas400adm.common.annotation;

/**
 * 操作日志操作常量（@OperateLog operation 值）。
 * 统一使用英文常量，避免中文硬编码。
 */
public final class OperateLogOperation {

    private OperateLogOperation() {}

    // ===== 通用操作 =====
    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";
    public static final String BATCH_DELETE = "BATCH_DELETE";
    public static final String TOGGLE_STATUS = "TOGGLE_STATUS";

    // ===== 用户管理 =====
    public static final String CREATE_USER = "CREATE_USER";
    public static final String UPDATE_USER = "UPDATE_USER";
    public static final String DELETE_USER = "DELETE_USER";
    public static final String ASSIGN_MENU = "ASSIGN_MENU";
    public static final String REMOVE_MENU = "REMOVE_MENU";
    public static final String UNLOCK_USER = "UNLOCK_USER";

    // ===== 角色管理 =====
    public static final String CREATE_ROLE = "CREATE_ROLE";
    public static final String UPDATE_ROLE = "UPDATE_ROLE";
    public static final String DELETE_ROLE = "DELETE_ROLE";
    public static final String BATCH_DELETE_ROLES = "BATCH_DELETE_ROLES";

    // ===== 菜单管理 =====
    public static final String CREATE_MENU = "CREATE_MENU";
    public static final String UPDATE_MENU = "UPDATE_MENU";
    public static final String DELETE_MENU = "DELETE_MENU";
    public static final String TOGGLE_MENU_STATUS = "TOGGLE_MENU_STATUS";

    // ===== 权限管理 =====
    public static final String CREATE_PERMISSION = "CREATE_PERMISSION";
    public static final String UPDATE_PERMISSION = "UPDATE_PERMISSION";
    public static final String DELETE_PERMISSION = "DELETE_PERMISSION";

    // ===== 权限申请 =====
    public static final String SUBMIT_PERMISSION_REQUEST = "SUBMIT_PERMISSION_REQUEST";
    public static final String APPROVE_PERMISSION_REQUEST = "APPROVE_PERMISSION_REQUEST";
    public static final String REJECT_PERMISSION_REQUEST = "REJECT_PERMISSION_REQUEST";

    // ===== 系统配置 =====
    public static final String UPDATE_CONFIG = "UPDATE_CONFIG";
    public static final String DELETE_CONFIG = "DELETE_CONFIG";

    // ===== 数据字典 =====
    public static final String CREATE_DICT_TYPE = "CREATE_DICT_TYPE";
    public static final String UPDATE_DICT_TYPE = "UPDATE_DICT_TYPE";
    public static final String DELETE_DICT_TYPE = "DELETE_DICT_TYPE";
    public static final String CREATE_DICT_ITEM = "CREATE_DICT_ITEM";
    public static final String UPDATE_DICT_ITEM = "UPDATE_DICT_ITEM";
    public static final String DELETE_DICT_ITEM = "DELETE_DICT_ITEM";

    // ===== 通知公告 =====
    public static final String PUBLISH_NOTICE = "PUBLISH_NOTICE";
    public static final String UPDATE_NOTICE = "UPDATE_NOTICE";
    public static final String DELETE_NOTICE = "DELETE_NOTICE";

    // ===== 通知中心 =====
    public static final String MARK_READ = "MARK_READ";
    public static final String MARK_ALL_READ = "MARK_ALL_READ";
    public static final String DELETE_NOTIFICATION = "DELETE_NOTIFICATION";
    public static final String BATCH_DELETE_NOTIFICATIONS = "BATCH_DELETE_NOTIFICATIONS";

    // ===== 仪表盘 =====
    public static final String UPDATE_WIDGET_VISIBILITY = "UPDATE_WIDGET_VISIBILITY";

    // ===== 快捷收藏 =====
    public static final String TOGGLE_FAVORITE = "TOGGLE_FAVORITE";
    public static final String CANCEL_FAVORITE = "CANCEL_FAVORITE";

    // ===== 日历 =====
    public static final String CREATE_CALENDAR_EVENT = "CREATE_CALENDAR_EVENT";
    public static final String UPDATE_CALENDAR_EVENT = "UPDATE_CALENDAR_EVENT";
    public static final String DELETE_CALENDAR_EVENT = "DELETE_CALENDAR_EVENT";

    // ===== 行政区划 =====
    public static final String CREATE_REGION = "CREATE_REGION";
    public static final String UPDATE_REGION = "UPDATE_REGION";
    public static final String DELETE_REGION = "DELETE_REGION";

    // ===== 知识库 =====
    public static final String CREATE_SYS_DOC = "CREATE_SYS_DOC";
    public static final String UPDATE_SYS_DOC = "UPDATE_SYS_DOC";
    public static final String DELETE_SYS_DOC = "DELETE_SYS_DOC";

    // ===== 认证 =====
    public static final String LOGIN = "LOGIN";
    public static final String LOGOUT = "LOGOUT";
    public static final String CHANGE_PASSWORD = "CHANGE_PASSWORD";

    // ===== AS400 管理 =====
    public static final String CREATE_SERVER = "CREATE_SERVER";
    public static final String UPDATE_SERVER = "UPDATE_SERVER";
    public static final String DELETE_SERVER = "DELETE_SERVER";
    public static final String TEST_CONNECTION = "TEST_CONNECTION";
    public static final String EXECUTE_CL = "EXECUTE_CL";

    // ===== IFS 文件 =====
    public static final String UPLOAD_TO_IFS = "UPLOAD_TO_IFS";
    public static final String CREATE_IFS_DIRECTORY = "CREATE_IFS_DIRECTORY";
    public static final String DELETE_IFS_FILE = "DELETE_IFS_FILE";
    public static final String RESTORE_IFS_FILE = "RESTORE_IFS_FILE";

    // ===== 文档管理 =====
    public static final String CREATE_DOC_TEMPLATE = "CREATE_DOC_TEMPLATE";
    public static final String UPDATE_DOC_TEMPLATE = "UPDATE_DOC_TEMPLATE";
    public static final String DELETE_DOC_TEMPLATE = "DELETE_DOC_TEMPLATE";
    public static final String CREATE_DOC = "CREATE_DOC";
    public static final String UPDATE_DOC = "UPDATE_DOC";
    public static final String DELETE_DOC = "DELETE_DOC";
    public static final String RESTORE_DOC = "RESTORE_DOC";
    public static final String PERMANENTLY_DELETE_DOC = "PERMANENTLY_DELETE_DOC";
    public static final String SUBMIT_FOR_APPROVAL = "SUBMIT_FOR_APPROVAL";
    public static final String APPROVE_DOC = "APPROVE_DOC";
    public static final String REJECT_DOC = "REJECT_DOC";
    public static final String ROLLBACK_VERSION = "ROLLBACK_VERSION";

    // ===== 数据区域 =====
    public static final String CREATE_DATA_AREA = "CREATE_DATA_AREA";
    public static final String UPDATE_DATA_AREA = "UPDATE_DATA_AREA";
    public static final String DELETE_DATA_AREA = "DELETE_DATA_AREA";

    // ===== 消息文件 =====
    public static final String CREATE_MESSAGE_DESCRIPTION = "CREATE_MESSAGE_DESCRIPTION";
    public static final String UPDATE_MESSAGE_DESCRIPTION = "UPDATE_MESSAGE_DESCRIPTION";
    public static final String DELETE_MESSAGE_DESCRIPTION = "DELETE_MESSAGE_DESCRIPTION";

    // ===== BPCS 运单 =====
    public static final String EXPORT_SHIPMENT_PDF = "EXPORT_SHIPMENT_PDF";

    // ===== BPCS 库存 =====
    public static final String CREATE_COUNT_PLAN = "CREATE_COUNT_PLAN";
    public static final String RECORD_COUNT_RESULT = "RECORD_COUNT_RESULT";

    // ===== 运费管理 =====
    public static final String CREATE_FREIGHT_RULE = "CREATE_FREIGHT_RULE";
    public static final String UPDATE_FREIGHT_RULE = "UPDATE_FREIGHT_RULE";
    public static final String DELETE_FREIGHT_RULE = "DELETE_FREIGHT_RULE";
    public static final String TOGGLE_FREIGHT_RULE = "TOGGLE_FREIGHT_RULE";
    public static final String CREATE_FREIGHT_RECORD = "CREATE_FREIGHT_RECORD";
    public static final String DELETE_FREIGHT_RECORD = "DELETE_FREIGHT_RECORD";

    // ===== 订单协同 =====
    public static final String CREATE_COLLABORATION = "CREATE_COLLABORATION";
    public static final String UPDATE_COLLABORATION_STATUS = "UPDATE_COLLABORATION_STATUS";
    public static final String ASSIGN_COLLABORATION = "ASSIGN_COLLABORATION";
    public static final String DELETE_COLLABORATION = "DELETE_COLLABORATION";
    public static final String SEND_COLLABORATION_NOTIFICATION = "SEND_COLLABORATION_NOTIFICATION";
    public static final String MARK_COLLABORATION_NOTIFICATION_READ = "MARK_COLLABORATION_NOTIFICATION_READ";

    // ===== 库存模拟 =====
    public static final String CREATE_SIMULATION = "CREATE_SIMULATION";
    public static final String RUN_SIMULATION = "RUN_SIMULATION";
    public static final String DELETE_SIMULATION = "DELETE_SIMULATION";

    // ===== 告警规则 =====
    public static final String CREATE_ALERT_RULE = "CREATE_ALERT_RULE";
    public static final String UPDATE_ALERT_RULE = "UPDATE_ALERT_RULE";
    public static final String DELETE_ALERT_RULE = "DELETE_ALERT_RULE";
    public static final String TOGGLE_ALERT_RULE = "TOGGLE_ALERT_RULE";

    // ===== Webhook =====
    public static final String CREATE_WEBHOOK = "CREATE_WEBHOOK";
    public static final String UPDATE_WEBHOOK = "UPDATE_WEBHOOK";
    public static final String DELETE_WEBHOOK = "DELETE_WEBHOOK";
    public static final String TOGGLE_WEBHOOK = "TOGGLE_WEBHOOK";
    public static final String TEST_WEBHOOK = "TEST_WEBHOOK";
    public static final String CLEAN_WEBHOOK_LOGS = "CLEAN_WEBHOOK_LOGS";
}
