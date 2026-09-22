-- V109: MRP 物料需求计划 - BOM展开、净需求计算、建议订单
-- 支持按销售订单/预测需求展开BOM，计算净需求，生成建议采购/生产订单

-- ---------- 1. BOM主表 ----------
CREATE TABLE IF NOT EXISTS `rx_bom_master` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `bom_no`            VARCHAR(32)     NOT NULL                 COMMENT 'BOM编号',
    `cono`              VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `parent_item`       VARCHAR(32)     NOT NULL                 COMMENT '父项物料编码',
    `parent_desc`       VARCHAR(200)    DEFAULT NULL             COMMENT '父项描述',
    `bom_version`       VARCHAR(16)     NOT NULL DEFAULT '1.0'   COMMENT 'BOM版本',
    `effective_date`    DATE            NOT NULL                 COMMENT '生效日期',
    `expiry_date`       DATE            DEFAULT NULL             COMMENT '失效日期',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
    `created_by`        VARCHAR(64)     NOT NULL,
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`      DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bom_no_version` (`bom_no`, `bom_version`),
    KEY `idx_parent_item` (`parent_item`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM主表';

-- ---------- 2. BOM行项表 ----------
CREATE TABLE IF NOT EXISTS `rx_bom_line` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `bom_id`            BIGINT          NOT NULL                 COMMENT 'BOM ID',
    `line_no`           INT             NOT NULL DEFAULT 0       COMMENT '行号',
    `component_item`    VARCHAR(32)     NOT NULL                 COMMENT '组件物料编码',
    `component_desc`    VARCHAR(200)    DEFAULT NULL             COMMENT '组件描述',
    `quantity`          DECIMAL(14,4)   NOT NULL                 COMMENT '单件用量',
    `uom`               VARCHAR(8)      DEFAULT 'EA'             COMMENT '单位',
    `scrap_rate`        DECIMAL(5,2)    DEFAULT 0.00             COMMENT '损耗率(%)',
    `efficiency`        DECIMAL(5,2)    DEFAULT 100.00           COMMENT '效率(%)',
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_bom_id` (`bom_id`),
    KEY `idx_component` (`component_item`),
    CONSTRAINT `fk_bom_line_bom` FOREIGN KEY (`bom_id`) REFERENCES `rx_bom_master` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM行项表';

-- ---------- 3. MRP需求表 ----------
CREATE TABLE IF NOT EXISTS `rx_mrp_demand` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `demand_no`         VARCHAR(32)     NOT NULL                 COMMENT '需求单号',
    `cono`              VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `demand_type`       VARCHAR(20)     NOT NULL                 COMMENT '需求类型：SALES_ORDER/FORECAST/Manual',
    `demand_source`     VARCHAR(32)     DEFAULT NULL             COMMENT '需求来源（订单号/预测号）',
    `item_code`         VARCHAR(32)     NOT NULL                 COMMENT '物料编码',
    `item_desc`         VARCHAR(200)    DEFAULT NULL             COMMENT '物料描述',
    `gross_requirement` DECIMAL(14,2)   NOT NULL                 COMMENT '毛需求',
    `scheduled_receipt` DECIMAL(14,2)   DEFAULT 0.00             COMMENT '在途收货',
    `allocated`         DECIMAL(14,2)   DEFAULT 0.00             COMMENT '已分配',
    `on_hand`           DECIMAL(14,2)   DEFAULT 0.00             COMMENT '现有库存',
    `safety_stock`      DECIMAL(14,2)   DEFAULT 0.00             COMMENT '安全库存',
    `net_requirement`   DECIMAL(14,2)   NOT NULL                 COMMENT '净需求',
    `required_date`     DATE            NOT NULL                 COMMENT '需求日期',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PLANNED/RELEASED/COMPLETED',
    `created_by`        VARCHAR(64)     NOT NULL,
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_demand_no` (`demand_no`),
    KEY `idx_item_code` (`item_code`),
    KEY `idx_required_date` (`required_date`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MRP需求表';

-- ---------- 4. MRP建议订单表 ----------
CREATE TABLE IF NOT EXISTS `rx_mrp_recommendation` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `recommendation_no` VARCHAR(32)     NOT NULL                 COMMENT '建议单号',
    `cono`              VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `demand_id`         BIGINT          NOT NULL                 COMMENT '关联需求ID',
    `item_code`         VARCHAR(32)     NOT NULL                 COMMENT '物料编码',
    `item_desc`         VARCHAR(200)    DEFAULT NULL             COMMENT '物料描述',
    `recommend_qty`     DECIMAL(14,2)   NOT NULL                 COMMENT '建议数量',
    `recommend_type`    VARCHAR(20)     NOT NULL                 COMMENT '建议类型：PURCHASE/PRODUCTION/TRANSFER',
    `lead_time_days`    INT             DEFAULT 0                COMMENT '提前期（天）',
    `suggested_date`    DATE            NOT NULL                 COMMENT '建议日期',
    `order_no`          VARCHAR(32)     DEFAULT NULL             COMMENT '关联订单号（已生成后）',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/RELEASED/CANCELLED',
    `created_by`        VARCHAR(64)     NOT NULL,
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_recommendation_no` (`recommendation_no`),
    KEY `idx_demand_id` (`demand_id`),
    KEY `idx_item_code` (`item_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MRP建议订单表';

-- ---------- 5. MRP运行日志表 ----------
CREATE TABLE IF NOT EXISTS `rx_mrp_run_log` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `run_no`            VARCHAR(32)     NOT NULL                 COMMENT '运行编号',
    `cono`              VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `run_date`          DATETIME        NOT NULL                 COMMENT '运行时间',
    `run_type`          VARCHAR(20)     NOT NULL                 COMMENT '运行类型：FULL/INCREMENTAL',
    `scope_items`       INT             DEFAULT 0                COMMENT '涉及物料数',
    `net_demand_count`  INT             DEFAULT 0                COMMENT '净需求数',
    `recommend_count`   INT             DEFAULT 0                COMMENT '建议单数',
    `duration_seconds`  INT             DEFAULT 0                COMMENT '运行时长（秒）',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS/FAILED',
    `error_message`     TEXT            COMMENT '错误信息',
    `created_by`        VARCHAR(64)     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_run_no` (`run_no`),
    KEY `idx_run_date` (`run_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MRP运行日志表';

-- ---------- 6. 权限码 ----------
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module, description)
VALUES ('MRP_BOM_VIEW', 'BOM View', 'mrp', 'BOM查看'),
       ('MRP_BOM_CREATE', 'BOM Create', 'mrp', 'BOM创建'),
       ('MRP_BOM_UPDATE', 'BOM Update', 'mrp', 'BOM更新'),
       ('MRP_RUN', 'MRP Run', 'mrp', 'MRP运行'),
       ('MRP_RECOMMENDATION_VIEW', 'Recommendation View', 'mrp', 'MRP建议查看'),
       ('MRP_RECOMMENDATION_RELEASE', 'Recommendation Release', 'mrp', 'MRP建议发布');

-- ---------- 7. 授权：ADMIN 全量权限 ----------
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code LIKE 'MRP_%';
