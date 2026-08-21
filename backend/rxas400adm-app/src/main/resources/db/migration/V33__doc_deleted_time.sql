-- ============================================================
-- V33: rx_doc 增加 deleted_time（记录逻辑删除时间，供「已删除文档」视图展示）
-- ============================================================
ALTER TABLE rx_doc
    ADD COLUMN deleted_time DATETIME NULL COMMENT '逻辑删除时间' AFTER deleted;
