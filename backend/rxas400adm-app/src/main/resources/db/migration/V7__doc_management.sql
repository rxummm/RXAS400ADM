-- 文档管理（3.9）：模板 / 文档（含审批状态机）/ 版本历史
CREATE TABLE rx_doc_template (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL COMMENT '模板名称',
    category     VARCHAR(50)  COMMENT '分类，如 运维/变更/发布',
    content      TEXT         COMMENT '模板正文（可含 {title} 占位）',
    created_by   VARCHAR(50),
    created_time DATETIME,
    updated_time DATETIME
) COMMENT = '文档模板';

CREATE TABLE rx_doc (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id   BIGINT       COMMENT '来源模板',
    title         VARCHAR(200) NOT NULL COMMENT '文档标题',
    content       LONGTEXT     COMMENT '正文',
    version       INT DEFAULT 1 COMMENT '当前版本',
    status        VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/PUBLISHED/REJECTED',
    reject_reason VARCHAR(500) COMMENT '驳回原因',
    created_by    VARCHAR(50),
    updated_by    VARCHAR(50),
    approved_by   VARCHAR(50),
    approved_time DATETIME,
    created_time  DATETIME,
    updated_time  DATETIME
) COMMENT = '运维文档（模板/版本/审批）';

CREATE TABLE rx_doc_version (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id       BIGINT NOT NULL COMMENT '文档 ID',
    version      INT    NOT NULL COMMENT '版本号',
    title        VARCHAR(200),
    content      LONGTEXT,
    operator     VARCHAR(50),
    created_time DATETIME
) COMMENT = '文档版本历史';
