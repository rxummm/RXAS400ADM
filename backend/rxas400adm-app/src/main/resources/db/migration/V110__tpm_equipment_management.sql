-- V110: 设备管理（TPM）- 设备台账、保养计划、OEE计算
-- 支持设备全生命周期管理、预防性维护、设备效率分析

-- ---------- 1. 设备台账表 ----------
CREATE TABLE IF NOT EXISTS `rx_equipment` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `equipment_no`          VARCHAR(32)     NOT NULL                 COMMENT '设备编号',
    `cono`                  VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `equipment_name`        VARCHAR(200)    NOT NULL                 COMMENT '设备名称',
    `equipment_type`        VARCHAR(50)     DEFAULT NULL             COMMENT '设备类型',
    `manufacturer`          VARCHAR(100)    DEFAULT NULL             COMMENT '制造商',
    `model`                 VARCHAR(50)     DEFAULT NULL             COMMENT '型号',
    `serial_no`             VARCHAR(50)     DEFAULT NULL             COMMENT '序列号',
    `location`              VARCHAR(100)    DEFAULT NULL             COMMENT '安装位置',
    `department`            VARCHAR(50)     DEFAULT NULL             COMMENT '使用部门',
    `purchase_date`         DATE            DEFAULT NULL             COMMENT '采购日期',
    `install_date`          DATE            DEFAULT NULL             COMMENT '安装日期',
    `warranty_expiry`       DATE            DEFAULT NULL             COMMENT '保修到期日',
    `status`                VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE/MAINTENANCE/SCRAPPED',
    `responsible_person`    VARCHAR(50)     DEFAULT NULL             COMMENT '责任人',
    `remark`                VARCHAR(500)    DEFAULT NULL             COMMENT '备注',
    `created_by`            VARCHAR(64)     NOT NULL,
    `created_time`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`          DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_equipment_no` (`equipment_no`),
    KEY `idx_status` (`status`),
    KEY `idx_location` (`location`),
    KEY `idx_department` (`department`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备台账表';

-- ---------- 2. 设备保养计划表 ----------
CREATE TABLE IF NOT EXISTS `rx_equipment_maintenance_plan` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `plan_no`           VARCHAR(32)     NOT NULL                 COMMENT '保养计划编号',
    `cono`              VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `equipment_id`      BIGINT          NOT NULL                 COMMENT '设备ID',
    `plan_name`         VARCHAR(200)    NOT NULL                 COMMENT '保养计划名称',
    `maintenance_type`  VARCHAR(20)     NOT NULL                 COMMENT '保养类型：DAILY/WEEKLY/MONTHLY/QUARTERLY/ANNUAL',
    `cycle_days`        INT             NOT NULL                 COMMENT '周期（天）',
    `next_due_date`     DATE            NOT NULL                 COMMENT '下次保养日期',
    `last_maintenance_date` DATE        DEFAULT NULL             COMMENT '上次保养日期',
    `responsible_person` VARCHAR(50)    DEFAULT NULL             COMMENT '责任人',
    `checklist`         TEXT            COMMENT '保养检查项（JSON）',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/SUSPENDED/COMPLETED',
    `created_by`        VARCHAR(64)     NOT NULL,
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`      DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_no` (`plan_no`),
    KEY `idx_equipment_id` (`equipment_id`),
    KEY `idx_next_due_date` (`next_due_date`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_maintenance_plan_equipment` FOREIGN KEY (`equipment_id`) REFERENCES `rx_equipment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备保养计划表';

-- ---------- 3. 设备保养记录表 ----------
CREATE TABLE IF NOT EXISTS `rx_equipment_maintenance_record` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `record_no`         VARCHAR(32)     NOT NULL                 COMMENT '保养记录编号',
    `plan_id`           BIGINT          NOT NULL                 COMMENT '保养计划ID',
    `equipment_id`      BIGINT          NOT NULL                 COMMENT '设备ID',
    `maintenance_date`  DATE            NOT NULL                 COMMENT '保养日期',
    `maintenance_type`  VARCHAR(20)     NOT NULL                 COMMENT '保养类型',
    `performed_by`      VARCHAR(50)     DEFAULT NULL             COMMENT '保养人员',
    `duration_hours`    DECIMAL(5,2)    DEFAULT NULL             COMMENT '保养时长（小时）',
    `parts_used`        VARCHAR(500)    DEFAULT NULL             COMMENT '更换配件',
    `cost`              DECIMAL(12,2)   DEFAULT NULL             COMMENT '保养费用',
    `findings`          TEXT            COMMENT '保养发现',
    `actions_taken`     TEXT            COMMENT '采取措施',
    `next_due_date`     DATE            DEFAULT NULL             COMMENT '下次保养日期',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'COMPLETED' COMMENT '状态：SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED',
    `created_by`        VARCHAR(64)     NOT NULL,
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_no` (`record_no`),
    KEY `idx_plan_id` (`plan_id`),
    KEY `idx_equipment_id` (`equipment_id`),
    KEY `idx_maintenance_date` (`maintenance_date`),
    CONSTRAINT `fk_maintenance_record_plan` FOREIGN KEY (`plan_id`) REFERENCES `rx_equipment_maintenance_plan` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备保养记录表';

-- ---------- 4. 设备故障记录表 ----------
CREATE TABLE IF NOT EXISTS `rx_equipment_failure` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `failure_no`        VARCHAR(32)     NOT NULL                 COMMENT '故障编号',
    `cono`              VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `equipment_id`      BIGINT          NOT NULL                 COMMENT '设备ID',
    `failure_date`      DATETIME        NOT NULL                 COMMENT '故障发生时间',
    `failure_type`      VARCHAR(50)     DEFAULT NULL             COMMENT '故障类型',
    `description`       TEXT            COMMENT '故障描述',
    `root_cause`        TEXT            COMMENT '根本原因',
    `repair_action`     TEXT            COMMENT '维修措施',
    `downtime_hours`    DECIMAL(8,2)    DEFAULT NULL             COMMENT '停机时长（小时）',
    `repair_cost`       DECIMAL(12,2)   DEFAULT NULL             COMMENT '维修费用',
    `resolved_date`     DATETIME        DEFAULT NULL             COMMENT '修复时间',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'OPEN'  COMMENT '状态：OPEN/IN_REPAIR/RESOLVED/CLOSED',
    `created_by`        VARCHAR(64)     NOT NULL,
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_failure_no` (`failure_no`),
    KEY `idx_equipment_id` (`equipment_id`),
    KEY `idx_failure_date` (`failure_date`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_equipment_failure` FOREIGN KEY (`equipment_id`) REFERENCES `rx_equipment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备故障记录表';

-- ---------- 5. OEE计算统计表 ----------
CREATE TABLE IF NOT EXISTS `rx_equipment_oee` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `equipment_id`      BIGINT          NOT NULL                 COMMENT '设备ID',
    `calc_date`         DATE            NOT NULL                 COMMENT '统计日期',
    `availability`      DECIMAL(5,2)    DEFAULT NULL             COMMENT '可用率(%)',
    `performance`       DECIMAL(5,2)    DEFAULT NULL             COMMENT '性能率(%)',
    `quality`           DECIMAL(5,2)    DEFAULT NULL             COMMENT '质量率(%)',
    `oee`               DECIMAL(5,2)    DEFAULT NULL             COMMENT 'OEE(%)',
    `planned_time`      DECIMAL(10,2)   DEFAULT NULL             COMMENT '计划运行时间（分钟）',
    `actual_runtime`    DECIMAL(10,2)   DEFAULT NULL             COMMENT '实际运行时间（分钟）',
    `downtime_minutes`  DECIMAL(10,2)   DEFAULT NULL             COMMENT '停机时间（分钟）',
    `total_count`       INT             DEFAULT NULL             COMMENT '总产量',
    `good_count`        INT             DEFAULT NULL             COMMENT '合格品数',
    `defect_count`      INT             DEFAULT NULL             COMMENT '缺陷品数',
    `created_by`        VARCHAR(64)     NOT NULL,
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_equipment_date` (`equipment_id`, `calc_date`),
    KEY `idx_calc_date` (`calc_date`),
    CONSTRAINT `fk_equipment_oee` FOREIGN KEY (`equipment_id`) REFERENCES `rx_equipment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OEE计算统计表';

-- ---------- 6. 权限码 ----------
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module, description)
VALUES ('EQUIPMENT_VIEW', 'Equipment View', 'tpm', '设备台账查看'),
       ('EQUIPMENT_CREATE', 'Equipment Create', 'tpm', '设备台账创建'),
       ('EQUIPMENT_UPDATE', 'Equipment Update', 'tpm', '设备台账更新'),
       ('MAINTENANCE_PLAN_VIEW', 'Maintenance Plan View', 'tpm', '保养计划查看'),
       ('MAINTENANCE_PLAN_CREATE', 'Maintenance Plan Create', 'tpm', '保养计划创建'),
       ('MAINTENANCE_RECORD_VIEW', 'Maintenance Record View', 'tpm', '保养记录查看'),
       ('FAILURE_RECORD_VIEW', 'Failure Record View', 'tpm', '故障记录查看'),
       ('OEE_VIEW', 'OEE View', 'tpm', 'OEE分析查看');

-- ---------- 7. 授权：ADMIN 全量权限 ----------
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND (p.permission_code LIKE 'EQUIPMENT_%' OR p.permission_code LIKE 'MAINTENANCE_%' OR p.permission_code LIKE 'FAILURE_%' OR p.permission_code = 'OEE_VIEW');
