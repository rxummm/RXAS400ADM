-- V71: 循环盘点支持（㉙）
-- 盘点计划表和盘点结果表

CREATE TABLE IF NOT EXISTS rx_cycle_count_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_no VARCHAR(20) NOT NULL COMMENT '计划编号',
    item VARCHAR(15) NOT NULL COMMENT '物料号',
    item_desc VARCHAR(50) COMMENT '物料描述',
    warehouse VARCHAR(4) NOT NULL COMMENT '仓库代码',
    planned_date DATE NOT NULL COMMENT '计划盘点日期',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/IN_PROGRESS/COMPLETED/CANCELLED',
    abc_class VARCHAR(1) COMMENT 'ABC 分类：A/B/C',
    operator VARCHAR(50) COMMENT '盘点人',
    created_by VARCHAR(50) NOT NULL COMMENT '创建人',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_plan_date (planned_date),
    INDEX idx_status (status),
    INDEX idx_item (item),
    UNIQUE KEY uk_plan_no (plan_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='循环盘点计划';

CREATE TABLE IF NOT EXISTS rx_cycle_count_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL COMMENT '关联计划ID',
    plan_no VARCHAR(20) NOT NULL COMMENT '计划编号',
    item VARCHAR(15) NOT NULL COMMENT '物料号',
    warehouse VARCHAR(4) NOT NULL COMMENT '仓库代码',
    system_qty INT NOT NULL DEFAULT 0 COMMENT '系统数量（BPCS IWI）',
    counted_qty INT NOT NULL DEFAULT 0 COMMENT '实盘数量',
    difference INT NOT NULL DEFAULT 0 COMMENT '差异 = counted_qty - system_qty',
    difference_value DECIMAL(12,2) DEFAULT 0 COMMENT '差异金额',
    reason VARCHAR(100) COMMENT '差异原因',
    counted_by VARCHAR(50) COMMENT '盘点人',
    count_time TIMESTAMP COMMENT '盘点时间',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_plan_id (plan_id),
    INDEX idx_item (item),
    INDEX idx_difference (difference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='循环盘点结果';
