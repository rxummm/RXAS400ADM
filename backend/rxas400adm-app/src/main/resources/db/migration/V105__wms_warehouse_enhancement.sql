-- V105: WMS 仓储管理增强 - 入库单、出库单、盘点单
-- 支持条码/RFID 扫描作业、仓库作业流程管理

-- ---------- 1. 入库单主表 ----------
CREATE TABLE IF NOT EXISTS `rx_wms_inbound` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `inbound_no`    VARCHAR(32)     NOT NULL                 COMMENT '入库单号',
    `cono`          VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `warehouse`     VARCHAR(16)     NOT NULL                 COMMENT '仓库编码',
    `inbound_type`  VARCHAR(20)     NOT NULL DEFAULT 'PURCHASE' COMMENT '入库类型：PURCHASE/RETURN/TRANSFER/ADJUST',
    `source_doc`    VARCHAR(32)     DEFAULT NULL             COMMENT '来源单据（PO/销售退货单等）',
    `expected_date` DATE            DEFAULT NULL             COMMENT '预计到货日期',
    `actual_date`   DATE            DEFAULT NULL             COMMENT '实际到货日期',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/RECEIVING/PUTAWAY/COMPLETED/CANCELLED',
    `operator`      VARCHAR(50)     DEFAULT NULL             COMMENT '操作员',
    `notes`         VARCHAR(500)    DEFAULT NULL             COMMENT '备注',
    `created_by`    VARCHAR(64)     NOT NULL                 COMMENT '创建人',
    `created_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`  DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inbound_no` (`inbound_no`),
    KEY `idx_warehouse` (`warehouse`),
    KEY `idx_status` (`status`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 入库单主表';

-- ---------- 2. 入库单行项表 ----------
CREATE TABLE IF NOT EXISTS `rx_wms_inbound_item` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `inbound_id`    BIGINT          NOT NULL                 COMMENT '入库单 ID',
    `line_no`       INT             NOT NULL DEFAULT 0       COMMENT '行号',
    `item_code`     VARCHAR(32)     NOT NULL                 COMMENT '物料编码',
    `item_desc`     VARCHAR(200)    DEFAULT NULL             COMMENT '物料描述',
    `qty_expected`  DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '预期数量',
    `qty_received`  DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '实收数量',
    `qty_rejected`  DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '拒收数量',
    `lot_no`        VARCHAR(32)     DEFAULT NULL             COMMENT '批次号',
    `expire_date`   DATE            DEFAULT NULL             COMMENT '有效期',
    `bin_no`        VARCHAR(32)     DEFAULT NULL             COMMENT '目标库位',
    `bar_code`      VARCHAR(64)     DEFAULT NULL             COMMENT '条码',
    `rfid_tag`      VARCHAR(64)     DEFAULT NULL             COMMENT 'RFID 标签',
    `created_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_inbound_id` (`inbound_id`),
    KEY `idx_item_code` (`item_code`),
    KEY `idx_lot_no` (`lot_no`),
    CONSTRAINT `fk_wms_inbound_item` FOREIGN KEY (`inbound_id`) REFERENCES `rx_wms_inbound` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 入库单行项表';

-- ---------- 3. 出库单主表 ----------
CREATE TABLE IF NOT EXISTS `rx_wms_outbound` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `outbound_no`   VARCHAR(32)     NOT NULL                 COMMENT '出库单号',
    `cono`          VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `warehouse`     VARCHAR(16)     NOT NULL                 COMMENT '仓库编码',
    `outbound_type` VARCHAR(20)     NOT NULL DEFAULT 'SHIPPING' COMMENT '出库类型：SHIPPING/TRANSFER/ADJUST',
    `source_doc`    VARCHAR(32)     DEFAULT NULL             COMMENT '来源单据（SO/调拨单等）',
    `customer_code` VARCHAR(32)     DEFAULT NULL             COMMENT '客户编码',
    `carrier`       VARCHAR(32)     DEFAULT NULL             COMMENT '承运商',
    `expected_date` DATE            DEFAULT NULL             COMMENT '预计出库日期',
    `actual_date`   DATE            DEFAULT NULL             COMMENT '实际出库日期',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PICKING/PACKING/SHIPPED/CANCELLED',
    `priority`      TINYINT         DEFAULT 0                COMMENT '优先级（0=普通，1=紧急）',
    `operator`      VARCHAR(50)     DEFAULT NULL             COMMENT '操作员',
    `notes`         VARCHAR(500)    DEFAULT NULL             COMMENT '备注',
    `created_by`    VARCHAR(64)     NOT NULL,
    `created_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`  DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_outbound_no` (`outbound_no`),
    KEY `idx_warehouse` (`warehouse`),
    KEY `idx_status` (`status`),
    KEY `idx_customer` (`customer_code`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 出库单主表';

-- ---------- 4. 出库单行项表 ----------
CREATE TABLE IF NOT EXISTS `rx_wms_outbound_item` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `outbound_id`   BIGINT          NOT NULL                 COMMENT '出库单 ID',
    `line_no`       INT             NOT NULL DEFAULT 0       COMMENT '行号',
    `item_code`     VARCHAR(32)     NOT NULL                 COMMENT '物料编码',
    `item_desc`     VARCHAR(200)    DEFAULT NULL             COMMENT '物料描述',
    `qty_requested` DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '请求数量',
    `qty_picked`    DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '拣货数量',
    `qty_shipped`   DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '发运数量',
    `lot_no`        VARCHAR(32)     DEFAULT NULL             COMMENT '批次号',
    `source_bin`    VARCHAR(32)     DEFAULT NULL             COMMENT '源库位',
    `bar_code`      VARCHAR(64)     DEFAULT NULL             COMMENT '条码',
    `rfid_tag`      VARCHAR(64)     DEFAULT NULL             COMMENT 'RFID 标签',
    `created_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_outbound_id` (`outbound_id`),
    KEY `idx_item_code` (`item_code`),
    CONSTRAINT `fk_wms_outbound_item` FOREIGN KEY (`outbound_id`) REFERENCES `rx_wms_outbound` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 出库单行项表';

-- ---------- 5. 盘点单主表 ----------
CREATE TABLE IF NOT EXISTS `rx_wms_count_plan` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `count_no`      VARCHAR(32)     NOT NULL                 COMMENT '盘点单号',
    `cono`          VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `warehouse`     VARCHAR(16)     NOT NULL                 COMMENT '仓库编码',
    `count_type`    VARCHAR(20)     NOT NULL DEFAULT 'CYCLE' COMMENT '类型：CYCLE/SYSTEMATIC/FULL',
    `count_date`    DATE            DEFAULT NULL             COMMENT '计划盘点日期',
    `actual_date`   DATE            DEFAULT NULL             COMMENT '实际盘点日期',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'PLANNED' COMMENT '状态：PLANNED/IN_PROGRESS/COMPLETED/CANCELLED',
    `counter`       VARCHAR(50)     DEFAULT NULL             COMMENT '盘点人',
    `checker`       VARCHAR(50)     DEFAULT NULL             COMMENT '复核人',
    `total_items`   INT             DEFAULT 0                COMMENT '应盘数量',
    `counted_items` INT             DEFAULT 0                COMMENT '已盘数量',
    `difference_count` INT          DEFAULT 0                COMMENT '差异项数',
    `notes`         VARCHAR(500)    DEFAULT NULL             COMMENT '备注',
    `created_by`    VARCHAR(64)     NOT NULL,
    `created_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`  DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_count_no` (`count_no`),
    KEY `idx_warehouse` (`warehouse`),
    KEY `idx_status` (`status`),
    KEY `idx_count_date` (`count_date`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 盘点单主表';

-- ---------- 6. 盘点单行项表 ----------
CREATE TABLE IF NOT EXISTS `rx_wms_count_item` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `count_id`      BIGINT          NOT NULL                 COMMENT '盘点单 ID',
    `line_no`       INT             NOT NULL DEFAULT 0       COMMENT '行号',
    `item_code`     VARCHAR(32)     NOT NULL                 COMMENT '物料编码',
    `item_desc`     VARCHAR(200)    DEFAULT NULL             COMMENT '物料描述',
    `warehouse`     VARCHAR(16)     NOT NULL                 COMMENT '仓库编码',
    `bin_no`        VARCHAR(32)     DEFAULT NULL             COMMENT '库位编码',
    `lot_no`        VARCHAR(32)     DEFAULT NULL             COMMENT '批次号',
    `system_qty`    DECIMAL(14,2)   NOT NULL DEFAULT 0.00    COMMENT '系统数量',
    `counted_qty`   DECIMAL(14,2)   DEFAULT NULL             COMMENT '实盘数量',
    `difference`    DECIMAL(14,2)   DEFAULT NULL             COMMENT '差异数量',
    `difference_value` DECIMAL(18,2) DEFAULT NULL            COMMENT '差异金额',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/COUNTED/DIFFERENCE',
    `counter`       VARCHAR(50)     DEFAULT NULL             COMMENT '盘点人',
    `counted_time`  DATETIME        DEFAULT NULL             COMMENT '盘点时间',
    `notes`         VARCHAR(500)    DEFAULT NULL             COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_count_id` (`count_id`),
    KEY `idx_item_code` (`item_code`),
    CONSTRAINT `fk_wms_count_item` FOREIGN KEY (`count_id`) REFERENCES `rx_wms_count_plan` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 盘点单行项表';

-- ---------- 7. 权限码 ----------
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module, description)
VALUES ('WMS_INBOUND_VIEW', 'WMS Inbound View', 'wms', 'WMS 入库单查看'),
       ('WMS_INBOUND_CREATE', 'WMS Inbound Create', 'wms', 'WMS 入库单创建'),
       ('WMS_INBOUND_UPDATE', 'WMS Inbound Update', 'wms', 'WMS 入库单更新'),
       ('WMS_OUTBOUND_VIEW', 'WMS Outbound View', 'wms', 'WMS 出库单查看'),
       ('WMS_OUTBOUND_CREATE', 'WMS Outbound Create', 'wms', 'WMS 出库单创建'),
       ('WMS_OUTBOUND_UPDATE', 'WMS Outbound Update', 'wms', 'WMS 出库单更新'),
       ('WMS_COUNT_VIEW', 'WMS Count View', 'wms', 'WMS 盘点单查看'),
       ('WMS_COUNT_CREATE', 'WMS Count Create', 'wms', 'WMS 盘点单创建'),
       ('WMS_COUNT_UPDATE', 'WMS Count Update', 'wms', 'WMS 盘点单更新'),
       ('WMS_BARCODE_SCAN', 'WMS Barcode Scan', 'wms', 'WMS 条码扫描作业');

-- ---------- 8. 授权：ADMIN 全量权限 ----------
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code LIKE 'WMS_%';
