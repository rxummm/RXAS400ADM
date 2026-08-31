-- V75: 运费核算、库存模拟、订单协同 三张业务表 + 菜单注册

-- ==================== 1. 运费核算规则表 ====================
CREATE TABLE IF NOT EXISTS rx_freight_cost_rule (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name   VARCHAR(100) NOT NULL COMMENT '规则名称',
    carrier     VARCHAR(50)  NOT NULL COMMENT '承运商',
    cost_type   VARCHAR(20)  NOT NULL COMMENT 'WEIGHT/VOLUME/PIECE',
    base_price  DECIMAL(12,2) NOT NULL COMMENT '基础价格',
    unit_price  DECIMAL(12,2) NOT NULL COMMENT '单位价格',
    min_price   DECIMAL(12,2) DEFAULT 0 COMMENT '最低价格',
    max_price   DECIMAL(12,2) DEFAULT 0 COMMENT '最高价格(0=不限)',
    enabled     TINYINT(1)   DEFAULT 1 COMMENT '启用标志',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    created_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运费计算规则';

-- ==================== 2. 运费记录表 ====================
CREATE TABLE IF NOT EXISTS rx_freight_cost_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no        VARCHAR(50)  NOT NULL COMMENT '订单号',
    carrier         VARCHAR(50)  NOT NULL COMMENT '承运商',
    weight          DECIMAL(10,2) DEFAULT 0 COMMENT '重量(kg)',
    volume          DECIMAL(10,2) DEFAULT 0 COMMENT '体积(m3)',
    piece_count     INT          DEFAULT 0 COMMENT '件数',
    estimated_cost  DECIMAL(12,2) DEFAULT 0 COMMENT '预估运费',
    actual_cost     DECIMAL(12,2) DEFAULT 0 COMMENT '实际运费',
    cost_diff       DECIMAL(12,2) DEFAULT 0 COMMENT '差异(实际-预估)',
    ship_date       DATE         DEFAULT NULL COMMENT '发运日期',
    created_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运费记录';

-- ==================== 3. 库存模拟表 ====================
CREATE TABLE IF NOT EXISTS rx_inventory_simulation (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    sim_name        VARCHAR(100) NOT NULL COMMENT '模拟名称',
    item_no         VARCHAR(50)  NOT NULL COMMENT '物料号',
    warehouse       VARCHAR(20)  NOT NULL COMMENT '仓库',
    current_stock   INT          DEFAULT 0 COMMENT '当前库存',
    demand_change   DECIMAL(5,2) DEFAULT 100 COMMENT '需求变化百分比',
    lead_time_days  INT          DEFAULT 7 COMMENT '提前期(天)',
    safety_stock    INT          DEFAULT 0 COMMENT '安全库存',
    reorder_point   INT          DEFAULT 0 COMMENT '再订货点',
    result_stockout_days   INT   DEFAULT 0 COMMENT '预计缺货天数',
    result_reorder_count   INT   DEFAULT 0 COMMENT '预计补货次数',
    result_avg_stock       INT   DEFAULT 0 COMMENT '预计平均库存',
    result_service_level   DECIMAL(5,2) DEFAULT 100 COMMENT '预计服务水平(%)',
    status          VARCHAR(20)  DEFAULT 'DRAFT' COMMENT 'DRAFT/RUNNING/COMPLETED',
    created_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存模拟';

-- ==================== 4. 订单协同表 ====================
CREATE TABLE IF NOT EXISTS rx_order_collaboration (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no        VARCHAR(50)  NOT NULL COMMENT '订单号',
    customer_code   VARCHAR(20)  DEFAULT NULL COMMENT '客户代码',
    customer_name   VARCHAR(100) DEFAULT NULL COMMENT '客户名称',
    status          VARCHAR(20)  DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/COMPLETED/CANCELLED',
    priority        VARCHAR(10)  DEFAULT 'NORMAL' COMMENT 'LOW/NORMAL/HIGH/URGENT',
    assigned_to     VARCHAR(50)  DEFAULT NULL COMMENT '负责人',
    due_date        DATE         DEFAULT NULL COMMENT '截止日期',
    notes           TEXT         DEFAULT NULL COMMENT '备注',
    created_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单协同';

-- ==================== 5. 协同通知表 ====================
CREATE TABLE IF NOT EXISTS rx_collaboration_notification (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    collaboration_id BIGINT     NOT NULL COMMENT '协同ID',
    sender          VARCHAR(50)  NOT NULL COMMENT '发送人',
    recipient       VARCHAR(50)  NOT NULL COMMENT '接收人',
    message         TEXT         NOT NULL COMMENT '消息内容',
    channel         VARCHAR(20)  DEFAULT 'SYSTEM' COMMENT 'SYSTEM/EMAIL/SMS',
    is_read         TINYINT(1)   DEFAULT 0 COMMENT '已读标志',
    created_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_collab_id (collaboration_id),
    INDEX idx_recipient (recipient, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协同通知';

-- ==================== 6. 菜单注册 ====================
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '运费成本分析', 2, 'bpcsFreightCost', '/bpcs-freight-cost', 'views/bpcs/freightCost/index.vue', 'BPCS_VIEW', 42, 'fa-solid fa-truck-fast', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsFreightCost' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '库存模拟仿真', 2, 'bpcsInventorySim', '/bpcs-inventory-sim', 'views/bpcs/inventorySim/index.vue', 'BPCS_VIEW', 43, 'fa-solid fa-flask', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsInventorySim' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, sort, icon, visible, status, admin_only, created_time, updated_time)
SELECT
    (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1),
    '订单协同', 2, 'bpcsOrderCollab', '/bpcs-order-collab', 'views/bpcs/orderCollab/index.vue', 'BPCS_VIEW', 44, 'fa-solid fa-people-arrows', 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsOrderCollab' AND menu_type = 2);

-- ==================== 7. 角色菜单授权 ====================
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN' AND m.title IN ('bpcsFreightCost', 'bpcsInventorySim', 'bpcsOrderCollab');

INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'OPERATOR' AND m.title IN ('bpcsFreightCost', 'bpcsInventorySim', 'bpcsOrderCollab');
