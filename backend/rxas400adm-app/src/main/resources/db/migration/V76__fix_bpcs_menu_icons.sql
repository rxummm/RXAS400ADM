-- V76: 修复 BPCS 菜单图标（V58~V69 创建时 icon 为 NULL）
-- 幂等：UPDATE WHERE icon IS NULL

-- V58: 订单时间轴
UPDATE rx_menu SET icon = 'fa-solid fa-clock-rotate-list' WHERE title = 'bpcsOrder' AND icon IS NULL;

-- V59: 客户档案、库存可用量、发运看板
UPDATE rx_menu SET icon = 'fa-solid fa-address-book' WHERE title = 'bpcsCustomer' AND icon IS NULL;
UPDATE rx_menu SET icon = 'fa-solid fa-warehouse' WHERE title = 'bpcsInventory' AND icon IS NULL;
UPDATE rx_menu SET icon = 'fa-solid fa-truck-ramp-box' WHERE title = 'bpcsShipping' AND icon IS NULL;

-- V60: 发票轨迹、销售趋势、采购订单
UPDATE rx_menu SET icon = 'fa-solid fa-file-invoice' WHERE title = 'bpcsInvoice' AND icon IS NULL;
UPDATE rx_menu SET icon = 'fa-solid fa-chart-line' WHERE title = 'bpcsSales' AND icon IS NULL;
UPDATE rx_menu SET icon = 'fa-solid fa-cart-shopping' WHERE title = 'bpcsPurchase' AND icon IS NULL;

-- V61: 物料主档
UPDATE rx_menu SET icon = 'fa-solid fa-boxes-stacked' WHERE title = 'bpcsItem' AND icon IS NULL;

-- V63: 订单列表、库存预警、销售分析
UPDATE rx_menu SET icon = 'fa-solid fa-list-check' WHERE title = 'bpcsOrderList' AND icon IS NULL;
UPDATE rx_menu SET icon = 'fa-solid fa-triangle-exclamation' WHERE title = 'bpcsInventoryAlert' AND icon IS NULL;
UPDATE rx_menu SET icon = 'fa-solid fa-chart-pie' WHERE title = 'bpcsSalesAnalysis' AND icon IS NULL;

-- V64: 库存变动、采购收货、发运列表、ABC分析、供应商绩效、供应链KPI、订单全链路
UPDATE rx_menu SET icon = 'fa-solid fa-clock-rotate-list' WHERE title = 'bpcsInventoryHistory' AND icon IS NULL;
UPDATE rx_menu SET icon = 'fa-solid fa-truck-loading' WHERE title = 'bpcsPurchaseReceiving' AND icon IS NULL;
UPDATE rx_menu SET icon = 'fa-solid fa-ship' WHERE title = 'bpcsShippingList' AND icon IS NULL;
UPDATE rx_menu SET icon = 'fa-solid fa-ranking-star' WHERE title = 'bpcsAbcAnalysis' AND icon IS NULL;
UPDATE rx_menu SET icon = 'fa-solid fa-star-half-stroke' WHERE title = 'bpcsSupplierPerf' AND icon IS NULL;
UPDATE rx_menu SET icon = 'fa-solid fa-gauge' WHERE title = 'bpcsKpi' AND icon IS NULL;
UPDATE rx_menu SET icon = 'fa-solid fa-route' WHERE title = 'bpcsOrderTracking' AND icon IS NULL;

-- V69: WMS 仓库管理
UPDATE rx_menu SET icon = 'fa-solid fa-warehouse' WHERE title = 'wmsOverview' AND icon IS NULL;
UPDATE rx_menu SET icon = 'fa-solid fa-border-all' WHERE title = 'wmsBinInventory' AND icon IS NULL;
UPDATE rx_menu SET icon = 'fa-solid fa-shoe-prints' WHERE title = 'wmsPickPath' AND icon IS NULL;
UPDATE rx_menu SET icon = 'fa-solid fa-barcode' WHERE title = 'wmsBatchTracking' AND icon IS NULL;
UPDATE rx_menu SET icon = 'fa-solid fa-arrows-turn-to-dots' WHERE title = 'wmsMovementHistory' AND icon IS NULL;
