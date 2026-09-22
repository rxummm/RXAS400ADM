-- V113: 菜单结构优化（三阶段）
-- Phase 1: BPCS 内部分组（采购/销售/库存/运输/需求/KPI）
-- Phase 2: 新增模块合并（生产管理/分析决策）+ 修复V80 bug + 修复排序冲突
-- Phase 3: 采购功能去重（合并 procurement 到 bpcsProcurement）
-- 幂等：全部 INSERT ... WHERE NOT EXISTS / UPDATE ... WHERE
-- 修复：用 SET @var 避免 MySQL 1093（UPDATE 不能在子查询中引用正在更新的表）

-- ============================================================
-- 预读 parent_id 到变量（避免 MySQL 1093）
-- ============================================================
SET @bpcs_id = (SELECT id FROM rx_menu WHERE title = 'bpcs' AND menu_type = 1 LIMIT 1);

-- ============================================================
-- Phase 1: BPCS 内部子分组
-- ============================================================

-- 1.1 创建 BPCS 子目录
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @bpcs_id, '采购管理', 1, 'bpcsProcurement', '', 'fa-solid fa-cart-shopping', 10, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsProcurement' AND menu_type = 1);

SET @procurement_id = (SELECT id FROM rx_menu WHERE title = 'bpcsProcurement' AND menu_type = 1 LIMIT 1);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @bpcs_id, '销售管理', 1, 'bpcsSalesGroup', '', 'fa-solid fa-chart-line', 20, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsSalesGroup' AND menu_type = 1);

SET @sales_id = (SELECT id FROM rx_menu WHERE title = 'bpcsSalesGroup' AND menu_type = 1 LIMIT 1);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @bpcs_id, '库存管理', 1, 'bpcsInventoryGroup', '', 'fa-solid fa-boxes-stacked', 30, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsInventoryGroup' AND menu_type = 1);

SET @inventory_id = (SELECT id FROM rx_menu WHERE title = 'bpcsInventoryGroup' AND menu_type = 1 LIMIT 1);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @bpcs_id, '运输管理', 1, 'bpcsShippingGroup', '', 'fa-solid fa-truck', 40, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsShippingGroup' AND menu_type = 1);

SET @shipping_id = (SELECT id FROM rx_menu WHERE title = 'bpcsShippingGroup' AND menu_type = 1 LIMIT 1);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @bpcs_id, '需求计划', 1, 'bpcsDemand', '', 'fa-solid fa-chart-bar', 50, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsDemand' AND menu_type = 1);

SET @demand_id = (SELECT id FROM rx_menu WHERE title = 'bpcsDemand' AND menu_type = 1 LIMIT 1);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @bpcs_id, '供应链KPI', 1, 'bpcsKpiGroup', '', 'fa-solid fa-gauge-high', 60, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsKpiGroup' AND menu_type = 1);

SET @kpi_id = (SELECT id FROM rx_menu WHERE title = 'bpcsKpiGroup' AND menu_type = 1 LIMIT 1);

-- 1.2 移动叶子菜单到对应子目录
-- 采购管理
UPDATE rx_menu SET parent_id = @procurement_id, sort = 1, updated_time = NOW()
WHERE title = 'bpcsPurchase' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @procurement_id, sort = 2, updated_time = NOW()
WHERE title = 'bpcsPurchaseReceiving' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @procurement_id, sort = 3, updated_time = NOW()
WHERE title = 'bpcsSupplierPerf' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @procurement_id, sort = 4, updated_time = NOW()
WHERE title = 'bpcsPoLifecycle' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @procurement_id, sort = 5, updated_time = NOW()
WHERE title = 'bpcsSupplierScore' AND parent_id = @bpcs_id;

-- 销售管理
UPDATE rx_menu SET parent_id = @sales_id, sort = 1, updated_time = NOW()
WHERE title = 'bpcsOrder' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @sales_id, sort = 2, updated_time = NOW()
WHERE title = 'bpcsOrderList' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @sales_id, sort = 3, updated_time = NOW()
WHERE title = 'bpcsOrderTracking' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @sales_id, sort = 4, updated_time = NOW()
WHERE title = 'bpcsCustomer' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @sales_id, sort = 5, updated_time = NOW()
WHERE title = 'bpcsSalesAnalysis' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @sales_id, sort = 6, updated_time = NOW()
WHERE title = 'bpcsRcmx' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @sales_id, sort = 7, updated_time = NOW()
WHERE title = 'bpcsCreditHold' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @sales_id, sort = 8, updated_time = NOW()
WHERE title = 'bpcsKanban' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @sales_id, sort = 9, updated_time = NOW()
WHERE title = 'bpcsOrderTemplate' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @sales_id, sort = 10, updated_time = NOW()
WHERE title = 'bpcsOrderCopy' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @sales_id, sort = 11, updated_time = NOW()
WHERE title = 'bpcsOrderChange' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @sales_id, sort = 12, updated_time = NOW()
WHERE title = 'bpcsOrderReport' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @sales_id, sort = 13, updated_time = NOW()
WHERE title = 'bpcsRma' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @sales_id, sort = 14, updated_time = NOW()
WHERE title = 'bpcsOrderCollab' AND parent_id = @bpcs_id;

-- 库存管理
UPDATE rx_menu SET parent_id = @inventory_id, sort = 1, updated_time = NOW()
WHERE title = 'bpcsInventory' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @inventory_id, sort = 2, updated_time = NOW()
WHERE title = 'bpcsInventoryAlert' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @inventory_id, sort = 3, updated_time = NOW()
WHERE title = 'bpcsInventoryHistory' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @inventory_id, sort = 4, updated_time = NOW()
WHERE title = 'bpcsAbcAnalysis' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @inventory_id, sort = 5, updated_time = NOW()
WHERE title = 'bpcsLocationInv' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @inventory_id, sort = 6, updated_time = NOW()
WHERE title = 'bpcsStockValue' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @inventory_id, sort = 7, updated_time = NOW()
WHERE title = 'bpcsCycleCount' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @inventory_id, sort = 8, updated_time = NOW()
WHERE title = 'bpcsInventorySim' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @inventory_id, sort = 9, updated_time = NOW()
WHERE title = 'bpcsReplenishment' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @inventory_id, sort = 10, updated_time = NOW()
WHERE title = 'bpcsWhReplenish' AND parent_id = @bpcs_id;

-- 运输管理
UPDATE rx_menu SET parent_id = @shipping_id, sort = 1, updated_time = NOW()
WHERE title = 'bpcsShipping' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @shipping_id, sort = 2, updated_time = NOW()
WHERE title = 'bpcsShippingList' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @shipping_id, sort = 3, updated_time = NOW()
WHERE title = 'bpcsInvoice' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @shipping_id, sort = 4, updated_time = NOW()
WHERE title = 'bpcsShipmentMgmt' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @shipping_id, sort = 5, updated_time = NOW()
WHERE title = 'bpcsTransportDashboard' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @shipping_id, sort = 6, updated_time = NOW()
WHERE title = 'bpcsTms' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @shipping_id, sort = 7, updated_time = NOW()
WHERE title = 'bpcsFreightCost' AND parent_id = @bpcs_id;

-- 需求计划
UPDATE rx_menu SET parent_id = @demand_id, sort = 1, updated_time = NOW()
WHERE title = 'bpcsForecast' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @demand_id, sort = 2, updated_time = NOW()
WHERE title = 'bpcsOrderSchedule' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @demand_id, sort = 3, updated_time = NOW()
WHERE title = 'bpcsBom' AND parent_id = @bpcs_id;

-- 供应链KPI
UPDATE rx_menu SET parent_id = @kpi_id, sort = 1, updated_time = NOW()
WHERE title = 'bpcsKpi' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @kpi_id, sort = 2, updated_time = NOW()
WHERE title = 'bpcsAbcXyz' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @kpi_id, sort = 3, updated_time = NOW()
WHERE title = 'bpcsAnomaly' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @kpi_id, sort = 4, updated_time = NOW()
WHERE title = 'bpcsControlTower' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @kpi_id, sort = 5, updated_time = NOW()
WHERE title = 'bpcsCpfr' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @kpi_id, sort = 6, updated_time = NOW()
WHERE title = 'bpcsWabp' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @kpi_id, sort = 7, updated_time = NOW()
WHERE title = 'bpcsAlertEngine' AND parent_id = @bpcs_id;

-- 1.3 将 WMS items 从 bpcs 移到 bpcsInventoryGroup 下
UPDATE rx_menu SET parent_id = @inventory_id, sort = 20, updated_time = NOW()
WHERE title = 'wmsOverview' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @inventory_id, sort = 21, updated_time = NOW()
WHERE title = 'wmsBinInventory' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @inventory_id, sort = 22, updated_time = NOW()
WHERE title = 'wmsPickPath' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @inventory_id, sort = 23, updated_time = NOW()
WHERE title = 'wmsBatchTracking' AND parent_id = @bpcs_id;
UPDATE rx_menu SET parent_id = @inventory_id, sort = 24, updated_time = NOW()
WHERE title = 'wmsMovementHistory' AND parent_id = @bpcs_id;

-- 1.4 补录孤儿路由菜单
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @sales_id, '客户360°视图', 2, 'bpcsCustomerOverview', '/bpcs-customer-overview', 'views/bpcs/customerOverview/index.vue', 'BPCS_CUSTOMER_VIEW', NULL, 15, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsCustomerOverview' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @sales_id, '履行率与OTD', 2, 'bpcsOrderAnalytics', '/bpcs-order-analytics', 'views/bpcs/orderAnalytics/index.vue', 'BPCS_ORDER_VIEW', NULL, 16, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsOrderAnalytics' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @sales_id, '订单详情增强', 2, 'bpcsOrderDetail', '/bpcs-order-detail', 'views/bpcs/orderDetail/index.vue', 'BPCS_ORDER_VIEW', NULL, 17, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'bpcsOrderDetail' AND menu_type = 2);

-- ============================================================
-- Phase 2: 新增模块合并（生产管理 + 分析决策）
-- ============================================================

-- 2.1 创建「生产管理」一级目录
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '生产管理', 1, 'production', '/production', 'fa-solid fa-industry', 55, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'production' AND menu_type = 1);

SET @production_id = (SELECT id FROM rx_menu WHERE title = 'production' AND menu_type = 1 LIMIT 1);

-- 2.2 生产管理子目录
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @production_id, '物料管理', 1, 'prodMaterial', '', 'fa-solid fa-cubes', 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'prodMaterial' AND menu_type = 1);

SET @prodMaterial_id = (SELECT id FROM rx_menu WHERE title = 'prodMaterial' AND menu_type = 1 LIMIT 1);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @production_id, '质量管理', 1, 'prodQuality', '', 'fa-solid fa-check-circle', 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'prodQuality' AND menu_type = 1);

SET @prodQuality_id = (SELECT id FROM rx_menu WHERE title = 'prodQuality' AND menu_type = 1 LIMIT 1);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @production_id, '成本核算', 1, 'prodCost', '', 'fa-solid fa-calculator', 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'prodCost' AND menu_type = 1);

SET @prodCost_id = (SELECT id FROM rx_menu WHERE title = 'prodCost' AND menu_type = 1 LIMIT 1);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @production_id, '设备管理', 1, 'prodTpm', '', 'fa-solid fa-wrench', 4, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'prodTpm' AND menu_type = 1);

SET @prodTpm_id = (SELECT id FROM rx_menu WHERE title = 'prodTpm' AND menu_type = 1 LIMIT 1);

-- 2.3 质量模块菜单
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @prodMaterial_id, 'BOM管理', 2, 'mrpBom', '/mrp-bom', 'views/mrp/bom/index.vue', 'MRP_BOM_VIEW', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'mrpBom' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @prodMaterial_id, 'MRP需求', 2, 'mrpDemand', '/mrp-demand', 'views/mrp/demand/index.vue', 'MRP_RECOMMENDATION_VIEW', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'mrpDemand' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @prodMaterial_id, 'MRP建议', 2, 'mrpRecommendation', '/mrp-recommendation', 'views/mrp/recommendation/index.vue', 'MRP_RECOMMENDATION_VIEW', NULL, 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'mrpRecommendation' AND menu_type = 2);

-- 2.4 质量管理菜单
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @prodQuality_id, '质量检验', 2, 'qualityInspection', '/quality-inspection', 'views/quality/inspection/index.vue', 'QUALITY_INSPECTION_VIEW', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'qualityInspection' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @prodQuality_id, 'NCR管理', 2, 'qualityNcr', '/quality-ncr', 'views/quality/ncr/index.vue', 'QUALITY_NCR_VIEW', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'qualityNcr' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @prodQuality_id, 'SPC控制图', 2, 'qualitySpc', '/quality-spc', 'views/quality/spc/index.vue', 'QUALITY_SPC_VIEW', NULL, 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'qualitySpc' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @prodQuality_id, '批次追溯', 2, 'qualityTraceability', '/quality-traceability', 'views/quality/traceability/index.vue', 'QUALITY_TRACE_VIEW', NULL, 4, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'qualityTraceability' AND menu_type = 2);

-- 2.5 成本核算菜单
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @prodCost_id, '成本归集', 2, 'costCollection', '/cost-collection', 'views/cost/collection/index.vue', 'COST_COLLECTION_VIEW', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'costCollection' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @prodCost_id, '成本差异', 2, 'costVariance', '/cost-variance', 'views/cost/variance/index.vue', 'COST_VARIANCE_VIEW', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'costVariance' AND menu_type = 2);

-- 2.6 设备管理菜单
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @prodTpm_id, '设备台账', 2, 'tpmEquipment', '/tpm-equipment', 'views/tpm/equipment/index.vue', 'EQUIPMENT_VIEW', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'tpmEquipment' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @prodTpm_id, '保养计划', 2, 'tpmMaintenance', '/tpm-maintenance', 'views/tpm/maintenance/index.vue', 'MAINTENANCE_PLAN_VIEW', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'tpmMaintenance' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @prodTpm_id, 'OEE分析', 2, 'tpmOee', '/tpm-oee', 'views/tpm/oee/index.vue', 'OEE_VIEW', NULL, 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'tpmOee' AND menu_type = 2);

-- 2.7 创建「分析决策」一级目录
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT NULL, '分析决策', 1, 'analytics', '/analytics', 'fa-solid fa-chart-pie', 75, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'analytics' AND menu_type = 1);

SET @analytics_id = (SELECT id FROM rx_menu WHERE title = 'analytics' AND menu_type = 1 LIMIT 1);

-- 2.8 OLAP 菜单
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @analytics_id, '销售分析', 2, 'olapSales', '/olap-sales', 'views/olap/sales.vue', 'OLAP_SALES_VIEW', NULL, 1, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'olapSales' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @analytics_id, '库存分析', 2, 'olapInventory', '/olap-inventory', 'views/olap/inventory.vue', 'OLAP_INVENTORY_VIEW', NULL, 2, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'olapInventory' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @analytics_id, '采购分析', 2, 'olapPurchase', '/olap-purchase', 'views/olap/purchase.vue', 'OLAP_PURCHASE_VIEW', NULL, 3, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'olapPurchase' AND menu_type = 2);

-- 2.9 将 EDI 菜单移到 BPCS 运输管理下
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @shipping_id, 'EDI文档', 2, 'ediDocument', '/edi-document', 'views/edi/document/index.vue', 'EDI_DOCUMENT_VIEW', NULL, 8, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'ediDocument' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @shipping_id, 'EDI伙伴', 2, 'ediPartner', '/edi-partner', 'views/edi/partner/index.vue', 'EDI_PARTNER_VIEW', NULL, 9, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'ediPartner' AND menu_type = 2);

-- 2.10 修复财务管理父目录（V101 创建为 title='ar' 但 menu_name='财务管理'）
UPDATE rx_menu SET title = 'finance', menu_name = '财务管理', path = '/finance', icon = 'fa-solid fa-landmark', updated_time = NOW()
WHERE title = 'ar' AND menu_type = 1;

-- ============================================================
-- Phase 3: 采购功能去重
-- ============================================================

-- 3.1 将独立 procurement 菜单移入 bpcsProcurement
INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @procurement_id, '采购订单管理', 2, 'procurementPo', '/procurement-po', 'views/procurement/po/index.vue', 'PO_MANAGE', NULL, 10, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'procurementPo' AND menu_type = 2);

INSERT INTO rx_menu (parent_id, menu_name, menu_type, title, path, component, perms, icon, sort, visible, status, admin_only, created_time, updated_time)
SELECT @procurement_id, '采购审批中心', 2, 'procurementApproval', '/procurement-approval', 'views/procurement/approval/index.vue', 'PO_APPROVE', NULL, 11, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM rx_menu WHERE title = 'procurementApproval' AND menu_type = 2);

-- ============================================================
-- Fix: V80 bug — menu_type 应为 2（叶子）而非 1（目录）
-- ============================================================
UPDATE rx_menu SET menu_type = 2, updated_time = NOW()
WHERE title IN ('bpcsControlTower', 'bpcsCpfr', 'bpcsTms') AND menu_type = 1;

-- ============================================================
-- Fix: ADMIN 角色授权新菜单
-- ============================================================
INSERT IGNORE INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM rx_role r, rx_menu m
WHERE r.role_code = 'ADMIN'
  AND m.title IN (
    'bpcsProcurement', 'bpcsSalesGroup', 'bpcsInventoryGroup', 'bpcsShippingGroup', 'bpcsDemand', 'bpcsKpiGroup',
    'production', 'prodMaterial', 'prodQuality', 'prodCost', 'prodTpm',
    'analytics',
    'qualityInspection', 'qualityNcr', 'qualitySpc', 'qualityTraceability',
    'costCollection', 'costVariance',
    'mrpBom', 'mrpDemand', 'mrpRecommendation',
    'tpmEquipment', 'tpmMaintenance', 'tpmOee',
    'olapSales', 'olapInventory', 'olapPurchase',
    'ediDocument', 'ediPartner',
    'bpcsCustomerOverview', 'bpcsOrderAnalytics', 'bpcsOrderDetail',
    'procurementPo', 'procurementApproval'
  );
