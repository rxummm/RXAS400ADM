package com.rxas400adm.operation.enums;

/**
 * 审计日志操作模块枚举。
 * 用于 @OperateLog 注解的 module 参数，替代中文硬编码。
 */
public enum OperateLogModule {
    // AS400 系统管理
    AS400_SYSTEM("as400.system", "AS400系统管理"),
    AS400_JOB("as400.job", "作业管理"),
    AS400_METRIC("as400.metric", "监控指标"),
    AS400_ALERT("as400.alert", "告警管理"),
    AS400_AUDIT("as400.audit", "审计日志"),
    AS400_CONFIG("as400.config", "配置管理"),
    AS400_USER("as400.user", "用户管理"),

    // 业务管理
    PROCUREMENT("procurement", "采购管理"),
    FINANCE_AR("finance.ar", "应收账款"),
    FINANCE_AP("finance.ap", "应付账款"),

    // 质量管理
    QUALITY("quality", "质量管理"),
    QUALITY_INSPECTION("quality.inspection", "质量检验"),
    QUALITY_NCR("quality.ncr", "不合格品处理"),
    QUALITY_SPC("quality.spc", "SPC控制"),

    // 成本管理
    COST("cost", "成本管理"),
    COST_COLLECTION("cost.collection", "成本归集"),
    COST_VARIANCE("cost.variance", "成本差异"),

    // WMS
    WMS("wms", "仓储管理"),
    WMS_INBOUND("wms.inbound", "入库管理"),
    WMS_OUTBOUND("wms.outbound", "出库管理"),
    WMS_COUNT("wms.count", "盘点管理"),

    // TMS
    TMS("tms", "运输管理"),
    TMS_SHIPMENT("tms.shipment", "运单管理"),
    TMS_FREIGHT("tms.freight", "运费结算"),

    // MRP
    MRP("mrp", "物料需求计划"),
    MRP_BOM("mrp.bom", "BOM管理"),
    MRP_RUN("mrp.run", "MRP运算"),

    // TPM
    TPM("tpm", "设备管理"),
    TPM_EQUIPMENT("tpm.equipment", "设备台账"),
    TPM_MAINTENANCE("tpm.maintenance", "保养管理"),

    // EDI
    EDI("edi", "EDI集成"),
    EDI_DOCUMENT("edi.document", "EDI文档"),
    EDI_PARTNER("edi.partner", "EDI伙伴"),

    // OLAP
    OLAP("olap", "多维分析"),
    OLAP_SALES("olap.sales", "销售分析"),
    OLAP_INVENTORY("olap.inventory", "库存分析"),

    // 审批
    APPROVAL("approval", "审批管理"),

    // 系统
    SYSTEM("system", "系统管理"),
    USER("user", "用户管理"),
    ROLE("role", "角色管理"),
    PERMISSION("permission", "权限管理"),
    MENU("menu", "菜单管理"),
    I18N("i18n", "国际化");

    private final String code;
    private final String label;

    OperateLogModule(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
