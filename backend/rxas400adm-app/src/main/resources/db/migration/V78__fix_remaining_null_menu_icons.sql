-- V78: 修复剩余 NULL 图标菜单
UPDATE rx_menu SET icon = 'fa-solid fa-table-columns' WHERE title = 'dataAreas' AND (icon IS NULL OR icon = '');
UPDATE rx_menu SET icon = 'fa-solid fa-id-card' WHERE title = 'userProfiles' AND (icon IS NULL OR icon = '');
