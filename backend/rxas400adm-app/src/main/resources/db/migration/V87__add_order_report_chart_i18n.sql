-- V87: Add missing i18n keys for orderReport ECharts chart labels
-- These keys were hardcoded in orderReport/index.vue and need to be internationalized

-- zh-CN keys
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.lineFillRate', 'zh-CN', '行履行率', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.orderFillRate', 'zh-CN', '订单履行率', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.onTime', 'zh-CN', '准时', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.early', 'zh-CN', '提前', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.late', 'zh-CN', '延迟', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.anomalyType', 'zh-CN', '异常类型', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.creditHold', 'zh-CN', '信用Hold', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.stockShortage', 'zh-CN', '库存不足', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.priceHold', 'zh-CN', '价格Hold', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.shippingDelay', 'zh-CN', '发货延迟', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.other', 'zh-CN', '其他', 'bpcs');

-- en-US keys
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.lineFillRate', 'en-US', 'Line Fill Rate', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.orderFillRate', 'en-US', 'Order Fill Rate', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.onTime', 'en-US', 'On Time', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.early', 'en-US', 'Early', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.late', 'en-US', 'Late', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.anomalyType', 'en-US', 'Anomaly Type', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.creditHold', 'en-US', 'Credit Hold', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.stockShortage', 'en-US', 'Stock Shortage', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.priceHold', 'en-US', 'Price Hold', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.shippingDelay', 'en-US', 'Shipping Delay', 'bpcs');
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES ('bpcs.orderReport.other', 'en-US', 'Other', 'bpcs');
