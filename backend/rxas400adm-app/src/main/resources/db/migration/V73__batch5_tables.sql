-- V73: 第五批长期规划 - 订单模板/复制/变更/RMA/排程表

-- 订单模板表
CREATE TABLE IF NOT EXISTS rx_order_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    cono VARCHAR(3) NOT NULL DEFAULT '001' COMMENT '公司码',
    cust VARCHAR(10) COMMENT '客户代码',
    ship_to VARCHAR(10) COMMENT '收货点',
    remark TEXT COMMENT '备注',
    line_json TEXT COMMENT '行明细 JSON',
    use_count INT NOT NULL DEFAULT 0 COMMENT '使用次数',
    active CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '启用标志',
    created_by VARCHAR(50) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单模板表';

-- 订单复制记录表
CREATE TABLE IF NOT EXISTS rx_order_copy_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_cono VARCHAR(3) NOT NULL,
    source_orno VARCHAR(20) NOT NULL COMMENT '源订单号',
    target_orno VARCHAR(20) COMMENT '目标订单号',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    copied_by VARCHAR(50) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_time TIMESTAMP NULL,
    INDEX idx_source (source_cono, source_orno)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单复制记录表';

-- 订单变更记录表
CREATE TABLE IF NOT EXISTS rx_order_change (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cono VARCHAR(3) NOT NULL,
    orno VARCHAR(20) NOT NULL COMMENT '订单号',
    change_type VARCHAR(30) NOT NULL COMMENT '变更类型',
    field_name VARCHAR(50) NOT NULL COMMENT '变更字段',
    old_value VARCHAR(200) COMMENT '原值',
    new_value VARCHAR(200) COMMENT '新值',
    reason TEXT COMMENT '变更原因',
    changed_by VARCHAR(50) NOT NULL,
    changed_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order (cono, orno)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单变更记录表';

-- 退货 RMA 表
CREATE TABLE IF NOT EXISTS rx_rma (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rma_no VARCHAR(20) NOT NULL COMMENT 'RMA 单号',
    cono VARCHAR(3) NOT NULL DEFAULT '001',
    orno VARCHAR(20) COMMENT '原订单号',
    cust VARCHAR(10) COMMENT '客户代码',
    item VARCHAR(20) NOT NULL COMMENT '物料号',
    qty INT NOT NULL COMMENT '退货数量',
    reason VARCHAR(200) COMMENT '退货原因',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'RMA 状态',
    created_by VARCHAR(50) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rma_no (rma_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退货 RMA 表';

-- 订单排程表
CREATE TABLE IF NOT EXISTS rx_order_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cono VARCHAR(3) NOT NULL DEFAULT '001',
    orno VARCHAR(20) NOT NULL COMMENT '订单号',
    cust VARCHAR(10) COMMENT '客户代码',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    progress INT NOT NULL DEFAULT 0 COMMENT '进度百分比',
    priority INT NOT NULL DEFAULT 5 COMMENT '优先级 1-10',
    created_by VARCHAR(50) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_date (start_date, end_date),
    UNIQUE KEY uk_order_schedule (cono, orno)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单排程表';
