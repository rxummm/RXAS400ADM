-- V112: OLAP 多维分析 - 数据仓库视图 + 分析指标表
-- 支持销售、库存、采购多维度分析，预聚合提升查询性能

-- ---------- 1. 销售分析汇总表 ----------
CREATE TABLE IF NOT EXISTS `rx_olap_sales_summary` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `analysis_date`         DATE            NOT NULL                 COMMENT '分析日期',
    `cono`                  VARCHAR(8)      NOT NULL                 COMMENT '公司代码',
    `customer_code`         VARCHAR(32)     DEFAULT NULL             COMMENT '客户编码',
    `customer_group`        VARCHAR(50)     DEFAULT NULL             COMMENT '客户分组',
    `sales_area`            VARCHAR(50)     DEFAULT NULL             COMMENT '销售区域',
    `item_code`             VARCHAR(32)     DEFAULT NULL             COMMENT '物料编码',
    `item_category`         VARCHAR(50)     DEFAULT NULL             COMMENT '物料分类',
    `order_type`            VARCHAR(20)     DEFAULT NULL             COMMENT '订单类型',
    `total_revenue`         DECIMAL(18,2)   DEFAULT 0.00             COMMENT '总收入',
    `total_quantity`        DECIMAL(14,2)   DEFAULT 0.00             COMMENT '总数量',
    `order_count`           INT             DEFAULT 0                COMMENT '订单数',
    `line_count`            INT             DEFAULT 0                COMMENT '行数',
    `avg_order_value`       DECIMAL(18,2)   DEFAULT 0.00             COMMENT '平均订单金额',
    `created_time`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_analysis_date` (`analysis_date`),
    KEY `idx_customer` (`customer_code`),
    KEY `idx_item` (`item_code`),
    KEY `idx_cono` (`cono`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OLAP 销售分析汇总表';

-- ---------- 2. 库存分析汇总表 ----------
CREATE TABLE IF NOT EXISTS `rx_olap_inventory_summary` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `analysis_date`         DATE            NOT NULL                 COMMENT '分析日期',
    `cono`                  VARCHAR(8)      NOT NULL                 COMMENT '公司代码',
    `warehouse`             VARCHAR(16)     DEFAULT NULL             COMMENT '仓库编码',
    `item_code`             VARCHAR(32)     NOT NULL                 COMMENT '物料编码',
    `item_category`         VARCHAR(50)     DEFAULT NULL             COMMENT '物料分类',
    `on_hand_qty`           DECIMAL(14,2)   DEFAULT 0.00             COMMENT '现有库存',
    `allocated_qty`         DECIMAL(14,2)   DEFAULT 0.00             COMMENT '已分配',
    `on_order_qty`          DECIMAL(14,2)   DEFAULT 0.00             COMMENT '在途',
    `available_qty`         DECIMAL(14,2)   DEFAULT 0.00             COMMENT '可用',
    `unit_cost`             DECIMAL(18,4)   DEFAULT 0.00             COMMENT '单位成本',
    `stock_value`           DECIMAL(18,2)   DEFAULT 0.00             COMMENT '库存价值',
    `created_time`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_analysis_date` (`analysis_date`),
    KEY `idx_warehouse` (`warehouse`),
    KEY `idx_item` (`item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OLAP 库存分析汇总表';

-- ---------- 3. 采购分析汇总表 ----------
CREATE TABLE IF NOT EXISTS `rx_olap_purchase_summary` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `analysis_date`         DATE            NOT NULL                 COMMENT '分析日期',
    `cono`                  VARCHAR(8)      NOT NULL                 COMMENT '公司代码',
    `vendor_code`           VARCHAR(32)     DEFAULT NULL             COMMENT '供应商编码',
    `vendor_group`          VARCHAR(50)     DEFAULT NULL             COMMENT '供应商分组',
    `item_code`             VARCHAR(32)     DEFAULT NULL             COMMENT '物料编码',
    `item_category`         VARCHAR(50)     DEFAULT NULL             COMMENT '物料分类',
    `total_amount`          DECIMAL(18,2)   DEFAULT 0.00             COMMENT '总金额',
    `total_quantity`        DECIMAL(14,2)   DEFAULT 0.00             COMMENT '总数量',
    `order_count`           INT             DEFAULT 0                COMMENT '订单数',
    `avg_lead_time_days`    DECIMAL(5,2)    DEFAULT NULL             COMMENT '平均提前期（天）',
    `created_time`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_analysis_date` (`analysis_date`),
    KEY `idx_vendor` (`vendor_code`),
    KEY `idx_item` (`item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OLAP 采购分析汇总表';

-- ---------- 4. 分析维度配置表 ----------
CREATE TABLE IF NOT EXISTS `rx_olap_dimension_config` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `dimension_code`    VARCHAR(32)     NOT NULL                 COMMENT '维度编码',
    `dimension_name`    VARCHAR(100)    NOT NULL                 COMMENT '维度名称',
    `dimension_type`    VARCHAR(20)     NOT NULL                 COMMENT '类型：HIERARCHY/FLAT',
    `parent_code`       VARCHAR(32)     DEFAULT NULL             COMMENT '父级编码',
    `level`             INT             DEFAULT 1                COMMENT '层级',
    `sort_order`        INT             DEFAULT 0                COMMENT '排序',
    `is_active`         TINYINT         DEFAULT 1                COMMENT '是否启用',
    `created_by`        VARCHAR(64)     NOT NULL,
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dimension_code` (`dimension_code`),
    KEY `idx_parent` (`parent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OLAP 维度配置表';

-- ---------- 5. 权限码 ----------
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module, description)
VALUES ('OLAP_SALES_VIEW', 'OLAP Sales View', 'olap', 'OLAP 销售分析查看'),
       ('OLAP_INVENTORY_VIEW', 'OLAP Inventory View', 'olap', 'OLAP 库存分析查看'),
       ('OLAP_PURCHASE_VIEW', 'OLAP Purchase View', 'olap', 'OLAP 采购分析查看'),
       ('OLAP_DIMENSION_MANAGE', 'OLAP Dimension Manage', 'olap', 'OLAP 维度配置管理');

-- ---------- 6. 授权：ADMIN 全量权限 ----------
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code LIKE 'OLAP_%';
