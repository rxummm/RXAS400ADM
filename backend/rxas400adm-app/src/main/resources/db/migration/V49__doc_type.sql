-- ============================================================
-- V49: 文档类型化 —— 按类型渲染（markdown/text/html 由正文渲染，pdf/图片 由 IFS 文件渲染）
-- rx_doc / rx_doc_template 增加 doc_type；存量数据默认 MARKDOWN（兼容现有 markdown 风格正文）。
-- 仅 ALTER 存量表加列，不新建对象，无需进 MANIFEST。
-- ============================================================
ALTER TABLE rx_doc
    ADD COLUMN doc_type VARCHAR(20) NOT NULL DEFAULT 'MARKDOWN'
    COMMENT '文档类型：MARKDOWN/TEXT/HTML/PDF/IMAGE（PDF/IMAGE 为 IFS 文件型，须上传到 IFS）' AFTER content;

ALTER TABLE rx_doc_template
    ADD COLUMN doc_type VARCHAR(20) NOT NULL DEFAULT 'MARKDOWN'
    COMMENT '模板类型：MARKDOWN/TEXT/HTML（应用模板时联动设置文档类型）' AFTER content;