-- ============================================================
-- V34: 纠正 V20 硬编码 id=47 导致的「审批管理」Tab 迁移失效（P0-7）
-- 背景：V20 用 `WHERE id = 47` 定位 permissionRequestReview 行，但自增主键
--       随插入顺序而定——全新库上该行 id 不是 47（作者开发库恰为 47），
--       UPDATE 匹配不到 → menu_type 保持 3（按钮）→ MenuService.userTabs()
--       只返回 menu_type=4 → 权限审批管理 Tab 在全新库上永远不出现。
-- 修复：改为按 title + menu_type 定位（不依赖 id），幂等——已迁移的行不受影响。
-- ============================================================

UPDATE rx_menu
SET menu_type = 4, updated_time = NOW()
WHERE title = 'permissionRequestReview' AND menu_type = 3;

-- 清理 V19/V22 中引用「menu 39 / 47」的过时注释（注释不参与执行，仅作记录说明，
-- 真正失效的 UPDATE 已按 title 定位，此处无额外数据变更）
