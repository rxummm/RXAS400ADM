-- V107: 质量追溯系统 - 质量检验、不合格品处理(NCR)、SPC控制图
-- 支持批次追溯、IQC/IPQC/OQC检验、纠正措施验证

-- ---------- 1. 质量检验记录表 ----------
CREATE TABLE IF NOT EXISTS `rx_quality_inspection` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `inspection_no`     VARCHAR(32)     NOT NULL                 COMMENT '检验单号',
    `cono`              VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `inspection_type`   VARCHAR(20)     NOT NULL                 COMMENT '检验类型：IQC/IPQC/OQC',
    `source_type`       VARCHAR(20)     DEFAULT NULL             COMMENT '来源类型：PO/SO/PRODUCTION',
    `source_no`         VARCHAR(32)     DEFAULT NULL             COMMENT '来源单号',
    `item_code`         VARCHAR(32)     NOT NULL                 COMMENT '物料编码',
    `item_desc`         VARCHAR(200)    DEFAULT NULL             COMMENT '物料描述',
    `batch_no`          VARCHAR(32)     DEFAULT NULL             COMMENT '批次号',
    `qty_inspected`     DECIMAL(14,2)   NOT NULL                 COMMENT '检验数量',
    `qty_accepted`      DECIMAL(14,2)   NOT NULL DEFAULT 0       COMMENT '合格数量',
    `qty_rejected`      DECIMAL(14,2)   NOT NULL DEFAULT 0       COMMENT '不合格数量',
    `result`            VARCHAR(20)     NOT NULL                 COMMENT '检验结果：PASS/FAIL/PARTIAL',
    `inspector`         VARCHAR(50)     DEFAULT NULL             COMMENT '检验员',
    `inspection_date`   DATE            NOT NULL                 COMMENT '检验日期',
    `defect_code`       VARCHAR(32)     DEFAULT NULL             COMMENT '缺陷代码',
    `defect_desc`       VARCHAR(200)    DEFAULT NULL             COMMENT '缺陷描述',
    `ncr_no`            VARCHAR(32)     DEFAULT NULL             COMMENT '关联NCR单号',
    `remark`            VARCHAR(500)    DEFAULT NULL             COMMENT '备注',
    `created_by`        VARCHAR(64)     NOT NULL,
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`      DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inspection_no` (`inspection_no`),
    KEY `idx_item_code` (`item_code`),
    KEY `idx_batch_no` (`batch_no`),
    KEY `idx_inspection_type` (`inspection_type`),
    KEY `idx_result` (`result`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量检验记录表';

-- ---------- 2. 不合格品报告表(NCR) ----------
CREATE TABLE IF NOT EXISTS `rx_ncr` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `ncr_no`                VARCHAR(32)     NOT NULL                 COMMENT 'NCR单号',
    `cono`                  VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `inspection_id`         BIGINT          DEFAULT NULL             COMMENT '关联检验单ID',
    `item_code`             VARCHAR(32)     NOT NULL                 COMMENT '物料编码',
    `item_desc`             VARCHAR(200)    DEFAULT NULL             COMMENT '物料描述',
    `batch_no`              VARCHAR(32)     DEFAULT NULL             COMMENT '批次号',
    `qty_rejected`          DECIMAL(14,2)   NOT NULL                 COMMENT '不合格数量',
    `defect_type`           VARCHAR(50)     DEFAULT NULL             COMMENT '缺陷类型',
    `defect_description`    TEXT            COMMENT '缺陷描述',
    `disposition`           VARCHAR(20)     DEFAULT NULL             COMMENT '处置方式：USE_AS_IS/REWORK/SCRAP/RETURN',
    `disposition_date`      DATE            DEFAULT NULL             COMMENT '处置日期',
    `root_cause`            TEXT            COMMENT '根本原因分析',
    `corrective_action`     TEXT            COMMENT '纠正措施',
    `preventive_action`     TEXT            COMMENT '预防措施',
    `assigned_to`           VARCHAR(50)     DEFAULT NULL             COMMENT '责任人',
    `due_date`              DATE            DEFAULT NULL             COMMENT '截止日期',
    `status`                VARCHAR(20)     NOT NULL DEFAULT 'OPEN'  COMMENT '状态：OPEN/IN_PROGRESS/CLOSED/CANCELLED',
    `close_date`            DATE            DEFAULT NULL             COMMENT '关闭日期',
    `close_remark`          VARCHAR(500)    DEFAULT NULL             COMMENT '关闭备注',
    `created_by`            VARCHAR(64)     NOT NULL,
    `created_time`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`          DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ncr_no` (`ncr_no`),
    KEY `idx_item_code` (`item_code`),
    KEY `idx_batch_no` (`batch_no`),
    KEY `idx_status` (`status`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不合格品报告表(NCR)';

-- ---------- 3. SPC控制图记录表 ----------
CREATE TABLE IF NOT EXISTS `rx_spc_record` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `item_code`         VARCHAR(32)     NOT NULL                 COMMENT '物料编码',
    `quality_char`      VARCHAR(100)    NOT NULL                 COMMENT '质量特性',
    `subgroup_size`     INT             NOT NULL DEFAULT 5       COMMENT '子组大小',
    `sample_date`       DATE            NOT NULL                 COMMENT '抽样日期',
    `subgroup_no`       INT             NOT NULL                 COMMENT '子组编号',
    `value_1`           DECIMAL(10,4)   DEFAULT NULL             COMMENT '测量值1',
    `value_2`           DECIMAL(10,4)   DEFAULT NULL             COMMENT '测量值2',
    `value_3`           DECIMAL(10,4)   DEFAULT NULL             COMMENT '测量值3',
    `value_4`           DECIMAL(10,4)   DEFAULT NULL             COMMENT '测量值4',
    `value_5`           DECIMAL(10,4)   DEFAULT NULL             COMMENT '测量值5',
    `mean`              DECIMAL(10,4)   DEFAULT NULL             COMMENT '均值(X-bar)',
    `range`             DECIMAL(10,4)   DEFAULT NULL             COMMENT '极差(R)',
    `ucl`               DECIMAL(10,4)   DEFAULT NULL             COMMENT '上控制限(UCL)',
    `cl`                DECIMAL(10,4)   DEFAULT NULL             COMMENT '中心线(CL)',
    `lcl`               DECIMAL(10,4)   DEFAULT NULL             COMMENT '下控制限(LCL)',
    `is_out_of_control` TINYINT         DEFAULT 0                COMMENT '是否异常(0=正常,1=异常)',
    `remark`            VARCHAR(500)    DEFAULT NULL             COMMENT '备注',
    `created_by`        VARCHAR(64)     NOT NULL,
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_item_code` (`item_code`),
    KEY `idx_sample_date` (`sample_date`),
    KEY `idx_out_of_control` (`is_out_of_control`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SPC控制图记录表';

-- ---------- 4. 批次追溯链 ----------
CREATE TABLE IF NOT EXISTS `rx_traceability_chain` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `item_code`         VARCHAR(32)     NOT NULL                 COMMENT '物料编码',
    `batch_no`          VARCHAR(32)     NOT NULL                 COMMENT '批次号',
    `trace_type`        VARCHAR(20)     NOT NULL                 COMMENT '追溯类型：UPSTREAM/DOWNSTREAM',
    `source_type`       VARCHAR(20)     DEFAULT NULL             COMMENT '来源类型：RAW_MATERIAL/PRODUCTION/INSPECTION',
    `source_no`         VARCHAR(32)     DEFAULT NULL             COMMENT '来源单号',
    `target_type`       VARCHAR(20)     DEFAULT NULL             COMMENT '目标类型',
    `target_no`         VARCHAR(32)     DEFAULT NULL             COMMENT '目标单号',
    `relationship`      VARCHAR(100)    DEFAULT NULL             COMMENT '关系描述',
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_item_code` (`item_code`),
    KEY `idx_batch_no` (`batch_no`),
    KEY `idx_trace_type` (`trace_type`),
    KEY `idx_source` (`source_type`, `source_no`),
    KEY `idx_target` (`target_type`, `target_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批次追溯链';

-- ---------- 5. 权限码 ----------
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module, description)
VALUES ('QUALITY_INSPECTION_VIEW', 'Quality Inspection View', 'quality', '质量检验查看'),
       ('QUALITY_INSPECTION_CREATE', 'Quality Inspection Create', 'quality', '质量检验创建'),
       ('QUALITY_NCR_VIEW', 'NCR View', 'quality', 'NCR单查看'),
       ('QUALITY_NCR_CREATE', 'NCR Create', 'quality', 'NCR单创建'),
       ('QUALITY_NCR_UPDATE', 'NCR Update', 'quality', 'NCR单更新'),
       ('QUALITY_SPC_VIEW', 'SPC View', 'quality', 'SPC控制图查看'),
       ('QUALITY_TRACE_VIEW', 'Traceability View', 'quality', '批次追溯查看');

-- ---------- 6. 授权：ADMIN 全量权限 ----------
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code LIKE 'QUALITY_%';
