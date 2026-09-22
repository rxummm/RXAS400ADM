-- V111: EDI/AS2 集成 - 采购订单/发货通知/发票交换
-- 支持 EDI 850(采购订单)、855(订单确认)、856(发货通知)、810(发票) 交换

-- ---------- 1. EDI 文档主表 ----------
CREATE TABLE IF NOT EXISTS `rx_edi_document` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `document_no`           VARCHAR(32)     NOT NULL                 COMMENT '文档编号',
    `cono`                  VARCHAR(8)      NOT NULL DEFAULT '001'   COMMENT '公司代码',
    `partner_id`            BIGINT          NOT NULL                 COMMENT '伙伴ID',
    `partner_code`          VARCHAR(32)     NOT NULL                 COMMENT '伙伴编码',
    `partner_name`          VARCHAR(200)    DEFAULT NULL             COMMENT '伙伴名称',
    `document_type`         VARCHAR(20)     NOT NULL                 COMMENT '文档类型：850/855/856/810',
    `direction`             VARCHAR(20)     NOT NULL                 COMMENT '方向：INBOUND/OUTBOUND',
    `status`                VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/SUCCESS/FAILED/REJECTED',
    `raw_content`           MEDIUMTEXT      COMMENT '原始 EDI 内容',
    `parsed_data`           MEDIUMTEXT      COMMENT '解析后数据（JSON）',
    `error_message`         TEXT            COMMENT '错误信息',
    `processed_time`        DATETIME        DEFAULT NULL             COMMENT '处理时间',
    `created_by`            VARCHAR(64)     NOT NULL,
    `created_time`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`          DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_document_no` (`document_no`),
    KEY `idx_partner_id` (`partner_id`),
    KEY `idx_document_type` (`document_type`),
    KEY `idx_direction` (`direction`),
    KEY `idx_status` (`status`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='EDI 文档主表';

-- ---------- 2. EDI 文档行项表 ----------
CREATE TABLE IF NOT EXISTS `rx_edi_document_line` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `document_id`       BIGINT          NOT NULL                 COMMENT '文档ID',
    `line_no`           INT             NOT NULL DEFAULT 0       COMMENT '行号',
    `item_code`         VARCHAR(32)     DEFAULT NULL             COMMENT '物料编码',
    `item_desc`         VARCHAR(200)    DEFAULT NULL             COMMENT '物料描述',
    `quantity`          DECIMAL(14,2)   DEFAULT NULL             COMMENT '数量',
    `unit_price`        DECIMAL(18,4)   DEFAULT NULL             COMMENT '单价',
    `amount`            DECIMAL(18,2)   DEFAULT NULL             COMMENT '金额',
    `uom`               VARCHAR(8)      DEFAULT 'EA'             COMMENT '单位',
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_item_code` (`item_code`),
    CONSTRAINT `fk_edi_line_document` FOREIGN KEY (`document_id`) REFERENCES `rx_edi_document` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='EDI 文档行项表';

-- ---------- 3. EDI 伙伴表 ----------
CREATE TABLE IF NOT EXISTS `rx_edi_partner` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `partner_code`      VARCHAR(32)     NOT NULL                 COMMENT '伙伴编码',
    `partner_name`      VARCHAR(200)    NOT NULL                 COMMENT '伙伴名称',
    `partner_type`      VARCHAR(20)     NOT NULL                 COMMENT '类型：SUPPLIER/CUSTOMER',
    `edi_version`       VARCHAR(16)     DEFAULT NULL             COMMENT 'EDI 版本',
    `as2_url`           VARCHAR(500)    DEFAULT NULL             COMMENT 'AS2 URL',
    `as2_from_id`       VARCHAR(100)    DEFAULT NULL             COMMENT 'AS2 From ID',
    `as2_to_id`         VARCHAR(100)    DEFAULT NULL             COMMENT 'AS2 To ID',
    `as2_micalg`        VARCHAR(50)     DEFAULT NULL             COMMENT 'AS2 消息校验算法',
    `as2_encalgo`       VARCHAR(50)     DEFAULT NULL             COMMENT 'AS2 加密算法',
    `as2_cert_path`     VARCHAR(500)    DEFAULT NULL             COMMENT 'AS2 证书路径',
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
    `created_by`        VARCHAR(64)     NOT NULL,
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time`      DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_partner_code` (`partner_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='EDI 伙伴表';

-- ---------- 4. EDI 传输日志表 ----------
CREATE TABLE IF NOT EXISTS `rx_edi_transfer_log` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `transfer_no`       VARCHAR(32)     NOT NULL                 COMMENT '传输编号',
    `document_id`       BIGINT          NOT NULL                 COMMENT '文档ID',
    `partner_id`        BIGINT          NOT NULL                 COMMENT '伙伴ID',
    `direction`         VARCHAR(20)     NOT NULL                 COMMENT '方向：SENT/RECEIVED',
    `as2_message_id`    VARCHAR(100)    DEFAULT NULL             COMMENT 'AS2 消息ID',
    `as2_mdn_received`  TINYINT         DEFAULT 0                COMMENT '是否收到 MDN',
    `as2_mdn_type`      VARCHAR(20)     DEFAULT NULL             COMMENT 'MDN 类型',
    `http_status`       INT             DEFAULT NULL             COMMENT 'HTTP 状态码',
    `response_time_ms`  INT             DEFAULT NULL             COMMENT '响应时间（毫秒）',
    `error_message`     TEXT            COMMENT '错误信息',
    `sent_time`         DATETIME        DEFAULT NULL             COMMENT '发送时间',
    `received_time`     DATETIME        DEFAULT NULL             COMMENT '接收时间',
    `created_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_transfer_no` (`transfer_no`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_partner_id` (`partner_id`),
    KEY `idx_sent_time` (`sent_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='EDI 传输日志表';

-- ---------- 5. 权限码 ----------
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module, description)
VALUES ('EDI_DOCUMENT_VIEW', 'EDI Document View', 'edi', 'EDI文档查看'),
       ('EDI_DOCUMENT_SEND', 'EDI Document Send', 'edi', 'EDI文档发送'),
       ('EDI_PARTNER_VIEW', 'EDI Partner View', 'edi', 'EDI伙伴查看'),
       ('EDI_PARTNER_CREATE', 'EDI Partner Create', 'edi', 'EDI伙伴创建'),
       ('EDI_TRANSFER_LOG_VIEW', 'EDI Transfer Log View', 'edi', 'EDI传输日志查看');

-- ---------- 6. 授权：ADMIN 全量权限 ----------
INSERT IGNORE INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code LIKE 'EDI_%';
