-- ============================================================
-- V15: 快捷收藏（按用户）
--   侧边栏收藏列表 + 顶栏星标收藏当前页
-- ============================================================
CREATE TABLE IF NOT EXISTS rx_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username VARCHAR(50) NOT NULL COMMENT '用户',
    title VARCHAR(200) NOT NULL COMMENT '名称（i18n key 或文本）',
    path VARCHAR(200) NOT NULL COMMENT '跳转路径',
    icon VARCHAR(100) NULL COMMENT '图标（EP 组件名或 fa- 全名）',
    created_time DATETIME NULL COMMENT '收藏时间',
    UNIQUE KEY uk_fav_user_path (username, path)
) COMMENT '快捷收藏';
