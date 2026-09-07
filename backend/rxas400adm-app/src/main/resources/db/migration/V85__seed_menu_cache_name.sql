-- V85: 为 route.name ≠ component defineOptions.name 的菜单设置 cache_name
-- 这些页面的 Vue 组件 defineOptions({ name }) 与路由 name 不同，需要 cache_name 映射

-- Source → defineOptions name = 'SourceManager'（route name = 'Source'）
UPDATE rx_menu SET cache_name = 'SourceManager' WHERE path = 'source' AND cache_name IS NULL;

-- Region → defineOptions name = 'RegionManage'（route name = 'Region'）
UPDATE rx_menu SET cache_name = 'RegionManage' WHERE path = 'region' AND cache_name IS NULL;
