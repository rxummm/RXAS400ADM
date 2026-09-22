-- V116: 修复生产管理/分析决策等模块菜单图标缺失（V113 创建时 icon=NULL）

-- 生产管理 - 物料管理 (mrpBom/mrpDemand/mrpRecommendation)
UPDATE rx_menu SET icon = 'fa-solid fa-sitemap' WHERE title = 'mrpBom' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-chart-column' WHERE title = 'mrpDemand' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-lightbulb' WHERE title = 'mrpRecommendation' AND (icon IS NULL OR icon = '');

-- 生产管理 - 质量管理 (qualityInspection/qualityNcr/qualitySpc/qualityTraceability)
UPDATE rx_menu SET icon = 'fa-solid fa-clipboard-check' WHERE title = 'qualityInspection' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-triangle-exclamation' WHERE title = 'qualityNcr' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-chart-line' WHERE title = 'qualitySpc' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-magnifying-glass' WHERE title = 'qualityTraceability' AND (icon IS NULL OR icon = '');

-- 生产管理 - 成本核算 (costCollection/costVariance)
UPDATE rx_menu SET icon = 'fa-solid fa-coins' WHERE title = 'costCollection' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-scale-unbalanced' WHERE title = 'costVariance' AND (icon IS NULL OR icon = '');

-- 生产管理 - 设备管理 (tpmEquipment/tpmMaintenance/tpmOee)
UPDATE rx_menu SET icon = 'fa-solid fa-clipboard-list' WHERE title = 'tpmEquipment' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-wrench' WHERE title = 'tpmMaintenance' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-gauge' WHERE title = 'tpmOee' AND (icon IS NULL OR icon = '');

-- 分析决策 - OLAP (olapSales/olapInventory/olapPurchase)
UPDATE rx_menu SET icon = 'fa-solid fa-chart-line' WHERE title = 'olapSales' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-boxes-stacked' WHERE title = 'olapInventory' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-cart-shopping' WHERE title = 'olapPurchase' AND (icon IS NULL OR icon = '');

-- EDI (ediDocument/ediPartner)
UPDATE rx_menu SET icon = 'fa-solid fa-file-lines' WHERE title = 'ediDocument' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-handshake' WHERE title = 'ediPartner' AND (icon IS NULL OR icon = '');

-- BPCS Sales (bpcsCustomerOverview/bpcsOrderAnalytics/bpcsOrderDetail)
UPDATE rx_menu SET icon = 'fa-solid fa-users' WHERE title = 'bpcsCustomerOverview' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-chart-bar' WHERE title = 'bpcsOrderAnalytics' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-file-invoice' WHERE title = 'bpcsOrderDetail' AND (icon IS NULL OR icon = '');

-- BPCS Procurement (procurementPo/procurementApproval)
UPDATE rx_menu SET icon = 'fa-solid fa-file-invoice-dollar' WHERE title = 'procurementPo' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-user-check' WHERE title = 'procurementApproval' AND (icon IS NULL OR icon = '');
