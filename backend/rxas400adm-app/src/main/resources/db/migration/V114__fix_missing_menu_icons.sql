-- V114: 修复 V113 遗漏的菜单图标 + 补充 BPCS 子目录 icon
-- V113 创建的 bpcsProcurement/bpcsSalesGroup/bpcsInventoryGroup/bpcsShippingGroup/bpcsDemand/bpcsKpiGroup 的 icon 字段为 NULL
-- 幂等：全部 UPDATE ... WHERE (icon IS NULL OR icon = '')

-- BPCS 子目录图标
UPDATE rx_menu SET icon = 'fa-solid fa-file-invoice-dollar' WHERE title = 'bpcsProcurement' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-chart-line' WHERE title = 'bpcsSalesGroup' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-boxes-stacked' WHERE title = 'bpcsInventoryGroup' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-truck' WHERE title = 'bpcsShippingGroup' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-chart-bar' WHERE title = 'bpcsDemand' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-gauge-high' WHERE title = 'bpcsKpiGroup' AND (icon IS NULL OR icon = '');

-- 生产管理子目录图标
UPDATE rx_menu SET icon = 'fa-solid fa-clipboard-check' WHERE title = 'prodQuality' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-calculator' WHERE title = 'prodCost' AND (icon IS NULL OR icon = '');

-- 财务管理应收发票子菜单图标
UPDATE rx_menu SET icon = 'fa-solid fa-file-invoice' WHERE title = 'arInvoice' AND (icon IS NULL OR icon = '');
