-- V88: 补全前端硬编码选项值的 i18n key（emailLog/cycleCount/freightCost/wabp）

-- ============================================================
-- emailLog 渠道选项
-- ============================================================
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES
  ('emailLog.channelAlert',  'zh-CN', '告警',   'emailLog'),
  ('emailLog.channelAlert',  'en-US', 'Alert',  'emailLog'),
  ('emailLog.channelReport', 'zh-CN', '报表',   'emailLog'),
  ('emailLog.channelReport', 'en-US', 'Report', 'emailLog'),
  ('emailLog.channelManual', 'zh-CN', '手动',   'emailLog'),
  ('emailLog.channelManual', 'en-US', 'Manual', 'emailLog');

-- ============================================================
-- cycleCount 盘点状态选项
-- ============================================================
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES
  ('bpcs.cycleCount.statusPending',    'zh-CN', '待执行', 'bpcs'),
  ('bpcs.cycleCount.statusPending',    'en-US', 'Pending',    'bpcs'),
  ('bpcs.cycleCount.statusInProgress', 'zh-CN', '执行中', 'bpcs'),
  ('bpcs.cycleCount.statusInProgress', 'en-US', 'In Progress', 'bpcs'),
  ('bpcs.cycleCount.statusCompleted',  'zh-CN', '已完成', 'bpcs'),
  ('bpcs.cycleCount.statusCompleted',  'en-US', 'Completed',  'bpcs');

-- ============================================================
-- cycleCount ABC 分类选项
-- ============================================================
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES
  ('bpcs.abcXyz.classA', 'zh-CN', 'A 类（高价值）', 'bpcs'),
  ('bpcs.abcXyz.classA', 'en-US', 'Class A (High Value)', 'bpcs'),
  ('bpcs.abcXyz.classB', 'zh-CN', 'B 类（中价值）', 'bpcs'),
  ('bpcs.abcXyz.classB', 'en-US', 'Class B (Medium Value)', 'bpcs'),
  ('bpcs.abcXyz.classC', 'zh-CN', 'C 类（低价值）', 'bpcs'),
  ('bpcs.abcXyz.classC', 'en-US', 'Class C (Low Value)', 'bpcs');

-- ============================================================
-- freightCost 计价方式选项
-- ============================================================
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES
  ('bpcs.freight.costTypeWeight', 'zh-CN', '按重量', 'bpcs'),
  ('bpcs.freight.costTypeWeight', 'en-US', 'By Weight', 'bpcs'),
  ('bpcs.freight.costTypeVolume', 'zh-CN', '按体积', 'bpcs'),
  ('bpcs.freight.costTypeVolume', 'en-US', 'By Volume', 'bpcs'),
  ('bpcs.freight.costTypePiece',  'zh-CN', '按件数', 'bpcs'),
  ('bpcs.freight.costTypePiece',  'en-US', 'By Piece',  'bpcs');

-- ============================================================
-- wabp 星期选项
-- ============================================================
INSERT IGNORE INTO rx_i18n (i18n_key, lang, text, module) VALUES
  ('bpcs.wabp.monday',    'zh-CN', '周一', 'bpcs'),
  ('bpcs.wabp.monday',    'en-US', 'Monday',    'bpcs'),
  ('bpcs.wabp.tuesday',   'zh-CN', '周二', 'bpcs'),
  ('bpcs.wabp.tuesday',   'en-US', 'Tuesday',   'bpcs'),
  ('bpcs.wabp.wednesday', 'zh-CN', '周三', 'bpcs'),
  ('bpcs.wabp.wednesday', 'en-US', 'Wednesday', 'bpcs'),
  ('bpcs.wabp.thursday',  'zh-CN', '周四', 'bpcs'),
  ('bpcs.wabp.thursday',  'en-US', 'Thursday',  'bpcs'),
  ('bpcs.wabp.friday',    'zh-CN', '周五', 'bpcs'),
  ('bpcs.wabp.friday',    'en-US', 'Friday',    'bpcs'),
  ('bpcs.wabp.saturday',  'zh-CN', '周六', 'bpcs'),
  ('bpcs.wabp.saturday',  'en-US', 'Saturday',  'bpcs'),
  ('bpcs.wabp.sunday',    'zh-CN', '周日', 'bpcs'),
  ('bpcs.wabp.sunday',    'en-US', 'Sunday',    'bpcs');
