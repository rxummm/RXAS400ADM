-- V90: Unify all menu icons to Font Awesome solid style
-- Root cause: V38 seed used EP icons (Odometer/Monitor/Setting etc.) which are outline/stroke-based,
-- while V69-V79 added FA solid icons (fa-solid fa-*) which are filled/bold.
-- This migration converts ALL remaining EP icons to FA solid equivalents for visual consistency.

-- 顶级叶子
UPDATE rx_menu SET icon = 'fa-solid fa-gauge' WHERE icon = 'Odometer';
UPDATE rx_menu SET icon = 'fa-solid fa-desktop' WHERE icon = 'Monitor';

-- 监控中心
UPDATE rx_menu SET icon = 'fa-solid fa-kit-medical' WHERE icon = 'FirstAidKit';
UPDATE rx_menu SET icon = 'fa-solid fa-server' WHERE icon = 'Coin';

-- 作业与任务
UPDATE rx_menu SET icon = 'fa-solid fa-briefcase' WHERE icon = 'Suitcase';
UPDATE rx_menu SET icon = 'fa-solid fa-clock' WHERE icon = 'AlarmClock';
UPDATE rx_menu SET icon = 'fa-solid fa-copy' WHERE icon = 'Files';
UPDATE rx_menu SET icon = 'fa-solid fa-file-lines' WHERE icon = 'Document';

-- 数据与对象
UPDATE rx_menu SET icon = 'fa-solid fa-magnifying-glass' WHERE icon = 'Search';
UPDATE rx_menu SET icon = 'fa-solid fa-cube' WHERE icon = 'Box';
UPDATE rx_menu SET icon = 'fa-solid fa-folder-open' WHERE icon = 'FolderOpened';
UPDATE rx_menu SET icon = 'fa-solid fa-gear' WHERE icon = 'Setting';
UPDATE rx_menu SET icon = 'fa-solid fa-ticket' WHERE icon = 'Tickets';

-- 系统管理
UPDATE rx_menu SET icon = 'fa-solid fa-user' WHERE icon = 'User';
UPDATE rx_menu SET icon = 'fa-solid fa-user-shield' WHERE icon = 'UserFilled';
UPDATE rx_menu SET icon = 'fa-solid fa-calendar' WHERE icon = 'Calendar';

-- 顶级分组
UPDATE rx_menu SET icon = 'fa-solid fa-rocket' WHERE icon = 'Promotion';
UPDATE rx_menu SET icon = 'fa-solid fa-screwdriver-wrench' WHERE icon = 'Tools';
UPDATE rx_menu SET icon = 'fa-solid fa-briefcase' WHERE icon = 'Briefcase';

-- 其他
UPDATE rx_menu SET icon = 'fa-solid fa-bars' WHERE icon = 'Menu';
UPDATE rx_menu SET icon = 'fa-solid fa-layer-group' WHERE icon = 'Collection';
UPDATE rx_menu SET icon = 'fa-solid fa-chart-line' WHERE icon = 'DataAnalysis';
UPDATE rx_menu SET icon = 'fa-solid fa-location-dot' WHERE icon = 'Location';
UPDATE rx_menu SET icon = 'fa-solid fa-book' WHERE icon = 'Notebook';
UPDATE rx_menu SET icon = 'fa-solid fa-share-nodes' WHERE icon = 'Share';

-- V67 email menus
UPDATE rx_menu SET icon = 'fa-solid fa-pen-to-square' WHERE icon = 'Edit';

-- Parent group menus (menu_type=1) with EP icons
UPDATE rx_menu SET icon = 'fa-solid fa-envelope' WHERE icon = 'Message';
UPDATE rx_menu SET icon = 'fa-solid fa-diagram-project' WHERE icon = 'Connection';

-- V69 WMS menus (EP icons that V76 didn't override due to WHERE icon IS NULL)
UPDATE rx_menu SET icon = 'fa-solid fa-house' WHERE icon = 'House';
UPDATE rx_menu SET icon = 'fa-solid fa-person-walking-with-cane' WHERE icon = 'Guide';
UPDATE rx_menu SET icon = 'fa-solid fa-arrow-down-wide-short' WHERE icon = 'Sort';
