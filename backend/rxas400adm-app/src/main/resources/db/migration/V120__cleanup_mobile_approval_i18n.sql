-- V120: 清理移动端审批中心 i18n（前端 mobile.vue 已删除）
DELETE FROM rx_i18n WHERE i18n_key LIKE 'approval.mobile.%';
