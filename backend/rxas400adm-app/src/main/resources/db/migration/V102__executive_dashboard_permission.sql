-- ============================================================
-- V100: Add EXECUTIVE_DASHBOARD permission and menu entry
-- ============================================================

-- ---------- 1. Add EXECUTIVE_DASHBOARD permission ----------
INSERT IGNORE INTO rx_permission (permission_code, permission_name, module, description)
VALUES ('EXECUTIVE_DASHBOARD', 'Executive Dashboard', 'dashboard', 'View executive dashboard with business KPIs');

-- ---------- 2. Grant to ADMIN role ----------
INSERT INTO rx_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM rx_role r, rx_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code = 'EXECUTIVE_DASHBOARD'
  AND NOT EXISTS (SELECT 1 FROM rx_role_permission WHERE role_id = r.id AND permission_id = p.id);

-- ---------- 3. Seed executive dashboard i18n translations ----------
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES
('dashboard.executive', 'zh-CN', 'Executive Dashboard', 'dashboard'),
('dashboard.executive', 'en-US', 'Executive Dashboard', 'dashboard'),
('dashboard.totalServers', 'zh-CN', '服务器总数', 'dashboard'),
('dashboard.totalServers', 'en-US', 'Total Servers', 'dashboard'),
('dashboard.onlineServers', 'zh-CN', '在线服务器', 'dashboard'),
('dashboard.onlineServers', 'en-US', 'Online Servers', 'dashboard'),
('dashboard.offlineServers', 'zh-CN', '离线服务器', 'dashboard'),
('dashboard.offlineServers', 'en-US', 'Offline Servers', 'dashboard'),
('dashboard.avgCpu', 'zh-CN', '平均 CPU', 'dashboard'),
('dashboard.avgCpu', 'en-US', 'Avg CPU', 'dashboard'),
('dashboard.avgMemory', 'zh-CN', '平均内存', 'dashboard'),
('dashboard.avgMemory', 'en-US', 'Avg Memory', 'dashboard'),
('dashboard.avgDisk', 'zh-CN', '平均磁盘', 'dashboard'),
('dashboard.avgDisk', 'en-US', 'Avg Disk', 'dashboard'),
('dashboard.activeAlerts', 'zh-CN', '活跃告警', 'dashboard'),
('dashboard.activeAlerts', 'en-US', 'Active Alerts', 'dashboard'),
('dashboard.totalOrders', 'zh-CN', '订单总数', 'dashboard'),
('dashboard.totalOrders', 'en-US', 'Total Orders', 'dashboard'),
('dashboard.closedOrders', 'zh-CN', '已完成订单', 'dashboard'),
('dashboard.closedOrders', 'en-US', 'Closed Orders', 'dashboard'),
('dashboard.completionRate', 'zh-CN', '完成率', 'dashboard'),
('dashboard.completionRate', 'en-US', 'Completion Rate', 'dashboard'),
('dashboard.totalItems', 'zh-CN', '物料总数', 'dashboard'),
('dashboard.totalItems', 'en-US', 'Total Items', 'dashboard'),
('dashboard.inventoryValue', 'zh-CN', '库存总值', 'dashboard'),
('dashboard.inventoryValue', 'en-US', 'Inventory Value', 'dashboard'),
('dashboard.onTimeDeliveryRate', 'zh-CN', '准时交付率', 'dashboard'),
('dashboard.onTimeDeliveryRate', 'en-US', 'On-Time Delivery', 'dashboard'),
('dashboard.salesTrend', 'zh-CN', '销售趋势', 'dashboard'),
('dashboard.salesTrend', 'en-US', 'Sales Trend', 'dashboard'),
('dashboard.topItems', 'zh-CN', '热销产品 TOP5', 'dashboard'),
('dashboard.topItems', 'en-US', 'Top Products TOP5', 'dashboard'),
('dashboard.topCustomers', 'zh-CN', '大客户 TOP5', 'dashboard'),
('dashboard.topCustomers', 'en-US', 'Top Customers TOP5', 'dashboard');
