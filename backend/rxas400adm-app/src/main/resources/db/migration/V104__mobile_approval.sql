-- V104: 移动审批功能 - 审批通知表
-- 统一审批通知，支持多业务类型

-- ---------- 1. 审批通知表 ----------
CREATE TABLE IF NOT EXISTS `rx_approval_notification` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `title`         VARCHAR(200)    NOT NULL                 COMMENT '通知标题',
    `content`       TEXT            COMMENT '通知内容',
    `target_type`   VARCHAR(50)     NOT NULL                 COMMENT '目标类型: PO, INVOICE, OPERATION...',
    `target_id`     BIGINT          NOT NULL                 COMMENT '目标ID',
    `approver_id`   BIGINT          NOT NULL                 COMMENT '审批人ID',
    `approver_name` VARCHAR(50)     NOT NULL                 COMMENT '审批人姓名',
    `status`        VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
    `action`        VARCHAR(20)     COMMENT 'APPROVED/REJECTED/RETURNED',
    `comment`       TEXT            COMMENT '审批意见',
    `created_by`    VARCHAR(50)     NOT NULL                 COMMENT '创建人',
    `created_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`  DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_approver_status` (`approver_id`, `status`),
    INDEX `idx_target` (`target_type`, `target_id`),
    INDEX `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批通知表';

-- ---------- 2. 审批人权限表（谁可以审批什么） ----------
CREATE TABLE IF NOT EXISTS `rx_approval_rule` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `rule_name`     VARCHAR(100)    NOT NULL                 COMMENT '规则名称',
    `target_type`   VARCHAR(50)     NOT NULL                 COMMENT '目标类型',
    `amount_min`    DECIMAL(15,2)   DEFAULT 0                COMMENT '金额下限',
    `amount_max`    DECIMAL(15,2)   COMMENT '金额上限',
    `level`         INT             NOT NULL DEFAULT 1       COMMENT '审批级别',
    `approver_type` VARCHAR(20)     NOT NULL DEFAULT 'ROLE'  COMMENT 'ROLE/USER',
    `approver_id`   BIGINT          NOT NULL                 COMMENT '审批人/角色ID',
    `enabled`       TINYINT         NOT NULL DEFAULT 1       COMMENT '是否启用',
    `created_by`    VARCHAR(50)     NOT NULL,
    `created_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_level` (`target_type`, `level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批规则表';
