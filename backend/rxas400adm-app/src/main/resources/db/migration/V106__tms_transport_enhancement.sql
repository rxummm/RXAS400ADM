-- V106: TMS 运输管理增强 - 运单状态机、GPS轨迹、签收凭证
-- 支持实时运输追踪、电子签收、运费结算

-- ---------- 1. 运单主表 ----------
CREATE TABLE IF NOT EXISTS `rx_tms_shipment` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `shipment_no`       VARCHAR(32)     NOT NULL                 COMMENT '运单号',
    `cono`              VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `carrier_code`      VARCHAR(16)     DEFAULT NULL             COMMENT '承运商编码',
    `carrier_name`      VARCHAR(100)    DEFAULT NULL             COMMENT '承运商名称',
    `vehicle_plate`     VARCHAR(20)     DEFAULT NULL             COMMENT '车牌号',
    `driver_name`       VARCHAR(50)     DEFAULT NULL             COMMENT '司机姓名',
    `driver_phone`      VARCHAR(20)     DEFAULT NULL             COMMENT '司机电话',
    `origin_warehouse`  VARCHAR(16)     DEFAULT NULL             COMMENT '起运仓库',
    `destination`       VARCHAR(200)    DEFAULT NULL             COMMENT '目的地址',
    `shipment_date`     DATE            DEFAULT NULL             COMMENT '发运日期',
    `estimated_arrival` DATE            DEFAULT NULL             COMMENT '预计到达日期',
    `actual_arrival`    DATE            DEFAULT NULL             COMMENT '实际到达日期',
    `weight_kg`         DECIMAL(10,2)   DEFAULT NULL             COMMENT '重量（kg）',
    `volume_m3`         DECIMAL(10,3)   DEFAULT NULL             COMMENT '体积（m³）',
    `piece_count`       INT             DEFAULT NULL             COMMENT '件数',
    `freight_cost`      DECIMAL(15,2)   DEFAULT NULL             COMMENT '运费',
    `insurance_cost`    DECIMAL(15,2)   DEFAULT NULL             COMMENT '保险费',
    `total_cost`        DECIMAL(15,2)   DEFAULT NULL             COMMENT '总费用',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PICKUP/IN_TRANSIT/ARRIVED/DELIVERED/CANCELLED',
    `signature`         VARCHAR(200)    DEFAULT NULL             COMMENT '签收人',
    `signature_photo`   VARCHAR(500)    DEFAULT NULL             COMMENT '签收凭证图片',
    `signature_time`    DATETIME        DEFAULT NULL             COMMENT '签收时间',
    `remark`            VARCHAR(500)    DEFAULT NULL             COMMENT '备注',
    `created_by`        VARCHAR(64)     NOT NULL,
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`      DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_shipment_no` (`shipment_no`),
    KEY `idx_carrier` (`carrier_code`),
    KEY `idx_status` (`status`),
    KEY `idx_shipment_date` (`shipment_date`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TMS 运单主表';

-- ---------- 2. 运单关联订单 ----------
CREATE TABLE IF NOT EXISTS `rx_tms_shipment_order` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `shipment_id`   BIGINT          NOT NULL                 COMMENT '运单 ID',
    `order_type`    VARCHAR(20)     NOT NULL                 COMMENT '订单类型：SO/PO/TRANSFER',
    `order_no`      VARCHAR(32)     NOT NULL                 COMMENT '订单号',
    `line_count`    INT             DEFAULT 0                COMMENT '行数',
    `qty_total`     DECIMAL(14,2)   DEFAULT 0.00             COMMENT '总数量',
    `amount`        DECIMAL(18,2)   DEFAULT 0.00             COMMENT '金额',
    `created_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_shipment_id` (`shipment_id`),
    KEY `idx_order` (`order_type`, `order_no`),
    CONSTRAINT `fk_tms_shipment_order` FOREIGN KEY (`shipment_id`) REFERENCES `rx_tms_shipment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TMS 运单关联订单';

-- ---------- 3. GPS 轨迹表 ----------
CREATE TABLE IF NOT EXISTS `rx_tms_gps_trace` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `shipment_id`   BIGINT          NOT NULL                 COMMENT '运单 ID',
    `latitude`      DECIMAL(10,7)   NOT NULL                 COMMENT '纬度',
    `longitude`     DECIMAL(10,7)   NOT NULL                 COMMENT '经度',
    `speed_kmh`     DECIMAL(8,2)    DEFAULT NULL             COMMENT '速度（km/h）',
    `heading`       TINYINT         DEFAULT NULL             COMMENT '方向（0-360）',
    `location_name` VARCHAR(100)    DEFAULT NULL             COMMENT '位置描述',
    `event_type`    VARCHAR(20)     DEFAULT 'LOCATION'       COMMENT '事件类型：LOCATION/STOP/DEPARTURE/ARRIVAL',
    `record_time`   DATETIME        NOT NULL                 COMMENT '记录时间',
    `created_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_shipment_id` (`shipment_id`),
    KEY `idx_record_time` (`record_time`),
    CONSTRAINT `fk_tms_gps_shipment` FOREIGN KEY (`shipment_id`) REFERENCES `rx_tms_shipment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TMS GPS 轨迹表';

-- ---------- 4. 运单事件日志 ----------
CREATE TABLE IF NOT EXISTS `rx_tms_shipment_event` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `shipment_id`   BIGINT          NOT NULL                 COMMENT '运单 ID',
    `event_type`    VARCHAR(20)     NOT NULL                 COMMENT '事件类型：PICKUP/DEPARTURE/IN_TRANSIT/ARRIVAL/DELIVERED',
    `event_time`    DATETIME        NOT NULL                 COMMENT '事件时间',
    `location`      VARCHAR(200)    DEFAULT NULL             COMMENT '位置',
    `operator`      VARCHAR(50)     DEFAULT NULL             COMMENT '操作人',
    `description`   VARCHAR(500)    DEFAULT NULL             COMMENT '描述',
    `photo_url`     VARCHAR(500)    DEFAULT NULL             COMMENT '凭证图片',
    `created_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_shipment_id` (`shipment_id`),
    KEY `idx_event_time` (`event_time`),
    CONSTRAINT `fk_tms_event_shipment` FOREIGN KEY (`shipment_id`) REFERENCES `rx_tms_shipment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TMS 运单事件日志';

-- ---------- 5. 运费结算表 ----------
CREATE TABLE IF NOT EXISTS `rx_tms_freight_settlement` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `settlement_no`     VARCHAR(32)     NOT NULL                 COMMENT '结算单号',
    `cono`              VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `carrier_code`      VARCHAR(16)     NOT NULL                 COMMENT '承运商编码',
    `carrier_name`      VARCHAR(100)    DEFAULT NULL             COMMENT '承运商名称',
    `settlement_period` VARCHAR(20)     NOT NULL                 COMMENT '结算周期（如 2026-09）',
    `total_shipments`   INT             DEFAULT 0                COMMENT '运单数',
    `total_weight_kg`   DECIMAL(12,2)   DEFAULT 0.00             COMMENT '总重量',
    `total_freight`     DECIMAL(15,2)   DEFAULT 0.00             COMMENT '运费合计',
    `total_insurance`   DECIMAL(15,2)   DEFAULT 0.00             COMMENT '保险费合计',
    `total_surcharge`   DECIMAL(15,2)   DEFAULT 0.00             COMMENT '附加费合计',
    `total_amount`      DECIMAL(15,2)   DEFAULT 0.00             COMMENT '结算总额',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/CONFIRMED/PAID',
    `paid_date`         DATE            DEFAULT NULL             COMMENT '付款日期',
    `remark`            VARCHAR(500)    DEFAULT NULL             COMMENT '备注',
    `created_by`        VARCHAR(64)     NOT NULL,
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`      DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_settlement_no` (`settlement_no`),
    KEY `idx_carrier` (`carrier_code`),
    KEY `idx_settlement_period` (`settlement_period`),
    KEY `idx_status` (`status`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TMS 运费结算表';

-- ---------- 6. 权限码 ----------
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module, description)
VALUES ('TMS_SHIPMENT_VIEW', 'TMS Shipment View', 'tms', 'TMS 运单查看'),
       ('TMS_SHIPMENT_CREATE', 'TMS Shipment Create', 'tms', 'TMS 运单创建'),
       ('TMS_SHIPMENT_UPDATE', 'TMS Shipment Update', 'tms', 'TMS 运单更新'),
       ('TMS_GPS_VIEW', 'TMS GPS View', 'tms', 'TMS GPS 轨迹查看'),
       ('TMS_SETTLEMENT_VIEW', 'TMS Settlement View', 'tms', 'TMS 运费结算查看'),
       ('TMS_SETTLEMENT_UPDATE', 'TMS Settlement Update', 'tms', 'TMS 运费结算更新');

-- ---------- 7. 授权：ADMIN 全量权限 ----------
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code LIKE 'TMS_%';
