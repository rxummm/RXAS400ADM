-- V92: 删除孤立表 rx_job_history / rx_order_copy_log
-- rx_job_history: V1 创建，无任何 Entity/Mapper/Service 引用，历史数据已无业务价值
-- rx_order_copy_log: V73 创建，OrderCopyController 不操作此表，仅为日志占位

DROP TABLE IF EXISTS rx_job_history;
DROP TABLE IF EXISTS rx_order_copy_log;
