-- V84: rx_menu 新增 cached / cache_name 字段，用于前端 keep-alive 配置
-- cached: 1=启用 keep-alive 缓存（默认），0=不缓存
-- cache_name: 可选，覆盖组件名用于 keep-alive include 匹配；为空时取前端路由 name

ALTER TABLE rx_menu
    ADD COLUMN cached TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 keep-alive 缓存，0=不缓存' AFTER visible,
    ADD COLUMN cache_name VARCHAR(64) NULL COMMENT 'keep-alive 组件名（覆盖前端路由 name）' AFTER cached;
