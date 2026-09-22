-- V108: 成本核算模块 - 成本归集、标准成本、差异分析
-- 支持按订单/产品/部门归集成本，标准成本vs实际成本差异分析

-- ---------- 1. 成本归集主表 ----------
CREATE TABLE IF NOT EXISTS `rx_cost_collection` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `collection_no`         VARCHAR(32)     NOT NULL                 COMMENT '归集单号',
    `cono`                  VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `cost_type`             VARCHAR(20)     NOT NULL                 COMMENT '成本类型：MATERIAL/LABOR/OVERHEAD',
    `cost_object_type`      VARCHAR(20)     NOT NULL                 COMMENT '成本对象类型：ORDER/PRODUCT/DEPARTMENT',
    `cost_object_no`        VARCHAR(32)     NOT NULL                 COMMENT '成本对象编号',
    `cost_object_name`      VARCHAR(200)    DEFAULT NULL             COMMENT '成本对象名称',
    `period`                VARCHAR(7)      NOT NULL                 COMMENT '成本期间(YYYY-MM)',
    `material_cost`         DECIMAL(18,2)   DEFAULT 0.00             COMMENT '材料成本',
    `labor_cost`            DECIMAL(18,2)   DEFAULT 0.00             COMMENT '人工成本',
    `overhead_cost`         DECIMAL(18,2)   DEFAULT 0.00             COMMENT '制造费用',
    `total_cost`            DECIMAL(18,2)   DEFAULT 0.00             COMMENT '总成本',
    `unit_cost`             DECIMAL(18,4)   DEFAULT 0.00             COMMENT '单位成本',
    `qty_produced`          DECIMAL(14,2)   DEFAULT 0.00             COMMENT '产量',
    `status`                VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/POSTED/CLOSED',
    `remark`                VARCHAR(500)    DEFAULT NULL             COMMENT '备注',
    `created_by`            VARCHAR(64)     NOT NULL,
    `created_time`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`          DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collection_no` (`collection_no`),
    KEY `idx_period` (`period`),
    KEY `idx_cost_type` (`cost_type`),
    KEY `idx_cost_object` (`cost_object_type`, `cost_object_no`),
    KEY `idx_status` (`status`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成本归集主表';

-- ---------- 2. 标准成本表 ----------
CREATE TABLE IF NOT EXISTS `rx_standard_cost` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `item_code`             VARCHAR(32)     NOT NULL                 COMMENT '物料编码',
    `item_desc`             VARCHAR(200)    DEFAULT NULL             COMMENT '物料描述',
    `cost_component`        VARCHAR(20)     NOT NULL                 COMMENT '成本组件：MATERIAL/LABOR/OVERHEAD',
    `standard_qty`          DECIMAL(14,2)   NOT NULL                 COMMENT '标准用量',
    `standard_price`        DECIMAL(18,4)   NOT NULL                 COMMENT '标准单价',
    `standard_cost`         DECIMAL(18,2)   NOT NULL                 COMMENT '标准成本',
    `effective_date`        DATE            NOT NULL                 COMMENT '生效日期',
    `expiry_date`           DATE            DEFAULT NULL             COMMENT '失效日期',
    `status`                VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
    `created_by`            VARCHAR(64)     NOT NULL,
    `created_time`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`          DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_item_code` (`item_code`),
    KEY `idx_effective_date` (`effective_date`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准成本表';

-- ---------- 3. 成本差异分析表 ----------
CREATE TABLE IF NOT EXISTS `rx_cost_variance` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `variance_no`           VARCHAR(32)     NOT NULL                 COMMENT '差异单号',
    `cono`                  VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `item_code`             VARCHAR(32)     NOT NULL                 COMMENT '物料编码',
    `item_desc`             VARCHAR(200)    DEFAULT NULL             COMMENT '物料描述',
    `cost_component`        VARCHAR(20)     NOT NULL                 COMMENT '成本组件：MATERIAL/LABOR/OVERHEAD',
    `standard_cost`         DECIMAL(18,2)   NOT NULL                 COMMENT '标准成本',
    `actual_cost`           DECIMAL(18,2)   NOT NULL                 COMMENT '实际成本',
    `variance_amount`       DECIMAL(18,2)   NOT NULL                 COMMENT '差异金额',
    `variance_pct`          DECIMAL(10,2)   DEFAULT NULL             COMMENT '差异率(%)',
    `variance_type`         VARCHAR(20)     NOT NULL                 COMMENT '差异类型：FAVORABLE/UNFAVORABLE',
    `period`                VARCHAR(7)      NOT NULL                 COMMENT '成本期间(YYYY-MM)',
    `root_cause`            VARCHAR(500)    DEFAULT NULL             COMMENT '差异原因',
    `improvement_action`    VARCHAR(500)    DEFAULT NULL             COMMENT '改善措施',
    `status`                VARCHAR(20)     NOT NULL DEFAULT 'OPEN'  COMMENT '状态：OPEN/IN_PROGRESS/CLOSED',
    `created_by`            VARCHAR(64)     NOT NULL,
    `created_time`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`          DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_variance_no` (`variance_no`),
    KEY `idx_item_code` (`item_code`),
    KEY `idx_period` (`period`),
    KEY `idx_cost_component` (`cost_component`),
    KEY `idx_variance_type` (`variance_type`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成本差异分析表';

-- ---------- 4. 利润分析表 ----------
CREATE TABLE IF NOT EXISTS `rx_profit_analysis` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `analysis_no`           VARCHAR(32)     NOT NULL                 COMMENT '分析单号',
    `cono`                  VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `analysis_type`         VARCHAR(20)     NOT NULL                 COMMENT '分析类型：PRODUCT/CUSTOMER/REGION',
    `analysis_key`          VARCHAR(32)     NOT NULL                 COMMENT '分析维度值',
    `analysis_name`         VARCHAR(200)    DEFAULT NULL             COMMENT '分析维度名称',
    `period`                VARCHAR(7)      NOT NULL                 COMMENT '分析期间(YYYY-MM)',
    `total_revenue`         DECIMAL(18,2)   DEFAULT 0.00             COMMENT '总收入',
    `total_cost`            DECIMAL(18,2)   DEFAULT 0.00             COMMENT '总成本',
    `gross_profit`          DECIMAL(18,2)   DEFAULT 0.00             COMMENT '毛利润',
    `profit_margin`         DECIMAL(10,2)   DEFAULT NULL             COMMENT '毛利率(%)',
    `order_count`           INT             DEFAULT 0                COMMENT '订单数',
    `item_count`            INT             DEFAULT 0                COMMENT '物料数',
    `status`                VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/COMPLETED',
    `remark`                VARCHAR(500)    DEFAULT NULL             COMMENT '备注',
    `created_by`            VARCHAR(64)     NOT NULL,
    `created_time`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_analysis_no` (`analysis_no`),
    KEY `idx_analysis_type` (`analysis_type`),
    KEY `idx_period` (`period`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='利润分析表';

-- ---------- 5. 权限码 ----------
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module, description)
VALUES ('COST_COLLECTION_VIEW', 'Cost Collection View', 'cost', '成本归集查看'),
       ('COST_COLLECTION_CREATE', 'Cost Collection Create', 'cost', '成本归集创建'),
       ('COST_COLLECTION_POST', 'Cost Collection Post', 'cost', '成本归集过账'),
       ('COST_STANDARD_VIEW', 'Standard Cost View', 'cost', '标准成本查看'),
       ('COST_STANDARD_UPDATE', 'Standard Cost Update', 'cost', '标准成本更新'),
       ('COST_VARIANCE_VIEW', 'Cost Variance View', 'cost', '成本差异查看'),
       ('COST_PROFIT_VIEW', 'Profit Analysis View', 'cost', '利润分析查看');

-- ---------- 6. 授权：ADMIN 全量权限 ----------
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code LIKE 'COST_%';
