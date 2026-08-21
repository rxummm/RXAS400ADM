-- ============================================================
-- V21: VIEWER（只读用户）补齐只读业务页面授权
--     背景：/jobs 等业务页是静态路由，未授权用户可直接打开页面，
--     但后端接口需要页面权限码（如 JOB_VIEW）→ 页面渲染但数据 403。
--     修法①：给 VIEWER 角色补齐全部只读业务页面（jobs/health/objects/
--            ifs/subsystems/pf/topology/docs/audit/executions/schedules/
--            scripts + 作业与任务目录），页面权限码随页面授权流入
--            （2.5.10 userMenuPerms），数据正常加载。
--     写操作按钮（JOB_END/SCRIPT_MANAGE/SCHEDULE_MANAGE/SUBSYSTEM_MANAGE/
--            DOC_MANAGE/DOC_APPROVE）不授予 VIEWER，仍保持只读。
--     修法②（前端）：路由守卫拦截未授权菜单页（见 router 改动）。
-- ============================================================
INSERT INTO rx_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM rx_role r CROSS JOIN rx_menu m
WHERE r.role_code = 'VIEWER'
  AND m.menu_type IN (1, 2)
  AND m.title IN ('groupJob', 'jobs', 'schedules', 'scripts', 'executions',
                  'health', 'assets', 'objects', 'ifs', 'subsystems',
                  'pf', 'topology', 'docs', 'audit')
  AND m.status = 1
  AND NOT EXISTS (SELECT 1 FROM rx_role_menu rm
                  WHERE rm.role_id = r.id AND rm.menu_id = m.id);
