-- V72: 第三批深度优化 - 运输管理
-- 注意：rx_alert_rule 已在 V1__init.sql 创建，不再重复

-- 运单主表
CREATE TABLE IF NOT EXISTS rx_shipment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_no VARCHAR(20) NOT NULL COMMENT '运单号',
    load_no VARCHAR(20) COMMENT '载荷号（BPCS LLH）',
    carrier VARCHAR(50) COMMENT '承运商',
    tracking_no VARCHAR(50) COMMENT '追踪号',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    ship_date DATE COMMENT '发运日期',
    eta DATE COMMENT '预计送达',
    actual_delivery DATE COMMENT '实际签收',
    order_no VARCHAR(20) COMMENT '关联订单号',
    created_by VARCHAR(50) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_shipment_no (shipment_no),
    INDEX idx_status (status),
    INDEX idx_load_no (load_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运单主表';

-- 运单事件表
CREATE TABLE IF NOT EXISTS rx_shipment_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    event_time TIMESTAMP NOT NULL,
    location VARCHAR(100),
    event_type VARCHAR(30) NOT NULL COMMENT '事件类型',
    description TEXT,
    operator VARCHAR(50),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_shipment_id (shipment_id),
    INDEX idx_event_time (event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运单事件表';

-- 承运商表
CREATE TABLE IF NOT EXISTS rx_carrier (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrier_code VARCHAR(20) NOT NULL COMMENT '承运商代码',
    carrier_name VARCHAR(100) NOT NULL COMMENT '承运商名称',
    contact VARCHAR(50) COMMENT '联系人',
    phone VARCHAR(30) COMMENT '电话',
    level VARCHAR(20) COMMENT '合作级别',
    region VARCHAR(100) COMMENT '运输区域',
    active CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '启用标志',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_carrier_code (carrier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='承运商表';


