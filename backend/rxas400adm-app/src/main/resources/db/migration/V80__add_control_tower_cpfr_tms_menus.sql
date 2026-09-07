-- V80: 新增菜单 - 控制塔 2.0 / CPFR 协同预测 / TMS Lite 运输管理
-- 控制塔 2.0 (OTIF + 中断预警 + 跨节点库存)
INSERT INTO rx_menu (parent_id, menu_name, title, icon, path, sort, menu_type, status)
SELECT id, 'bpcsControlTower', '控制塔 2.0', 'fa-solid fa-tower-control', '/bpcs-control-tower', 25, 1, 1
FROM (SELECT id FROM rx_menu WHERE path = '/bpcs-forecast' LIMIT 1) t;

-- CPFR 协同预测
INSERT INTO rx_menu (parent_id, menu_name, title, icon, path, sort, menu_type, status)
SELECT id, 'bpcsCpfr', '协同预测(CPFR)', 'fa-solid fa-brain', '/bpcs-cpfr', 12, 1, 1
FROM (SELECT id FROM rx_menu WHERE path = '/bpcs-forecast' LIMIT 1) t;

-- TMS Lite 运输管理
INSERT INTO rx_menu (parent_id, menu_name, title, icon, path, sort, menu_type, status)
SELECT id, 'bpcsTms', '运输管理(TMS)', 'fa-solid fa-truck-fast', '/bpcs-tms', 14, 1, 1
FROM (SELECT id FROM rx_menu WHERE path = '/bpcs-shipping' LIMIT 1) t;
