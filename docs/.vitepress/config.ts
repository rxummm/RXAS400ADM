import { defineConfig } from 'vitepress'

/**
 * RXAS400ADM 审计文档站（VitePress）
 *
 * === 文件管理约定 ===
 * srcExclude 使用 glob 模式（而非逐文件列举），避免文件增删后需要手动维护本配置。
 * 新增过程文档请按已有命名规范命名，即可被对应模式自动排除，无需修改此处。
 *
 * 过程文档命名规范（会被自动排除）：
 *   - 前后端全面分析报告-YYYY-MM-DD.md
 *   - 前后端全面测试步骤-YYYY-MM-DD.md
 *   - 项目上线风险评估和建议-YYYY-MM-DD.md
 *   - 项目增强和实现-YYYY-MM-DD.md
 *   - 项目前后端增强-YYYY-MM-DD.md
 *   - 项目邮件功能增强-YYYY-MM-DD.md
 *   - AS400业务部分增强-YYYY-MM-DD.md
 *   - 修复todo-list-YYYY-MM-DD.md
 *   - 详细实施指南-*-YYYY-MM-DD.md
 *   - US400CND部署检查清单-YYYY-MM-DD.md
 *   - 前后端测试报告-YYYY-MM-DD.md
 *
 * 永久参考文档（站点收录）：
 *   - index.md（首页）
 *   - RXAS400ADM-新人上手指南.md
 *   - RXAS400ADM-工具链正则避坑指南.md
 *   - review/trae/*.md（代码审计报告按章拆页）
 *   - flowcharts/*.md（流程图，排除 generated/ 和 modules-mermaid-source.md）
 */
export default defineConfig({
  lang: 'zh-CN',
  title: 'RXAS400ADM 审计与文档',
  description: '代码审计报告 / 复核备注 / 新人上手 / 门禁指南 / 系统流程图',
  cleanUrls: true,
  ignoreDeadLinks: true,
  markdown: {
    config: (md) => {
      const defaultFence = md.renderer.rules.fence!
      md.renderer.rules.fence = (tokens, idx, options, env, self) => {
        const token = tokens[idx]
        if (token.info.trim() === 'mermaid') {
          const encoded = token.content
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
          return '<pre class="mermaid">' + encoded + '</pre>'
        }
        return defaultFence(tokens, idx, options, env, self)
      }
    },
  },
  srcExclude: [
    // 自动生成 / 中间产物
    'flowcharts/generated/**',
    'flowcharts/modules-mermaid-source.md',
    // 模块流程图接入指南（内部参考，非站点内容）
    '模块流程图与菜单接入指南.md',
    // 过程文档 — 按日期迭代的临时分析/测试/实施报告，非永久参考文档
    // 新增同类文档只需按命名规范命名，无需修改本配置
    '前后端全面分析报告-*.md',
    '前后端全面测试步骤-*.md',
    '项目上线风险评估和建议-*.md',
    '项目增强和实现-*.md',
    '项目前后端增强-*.md',
    '项目邮件功能增强-*.md',
    'AS400业务部分增强-*.md',
    '修复todo-list-*.md',
    '详细实施指南-*.md',
    'US400CND部署检查清单-*.md',
    '前后端测试报告-*.md',
    '部署到AS400指南.md',
    'push project to github.md',
  ],
  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      { text: '系统流程图', link: '/flowcharts/' },
      { text: '代码审计（按章）', link: '/review/trae/01-直击痛点阻碍新人理解的-top-5-核心坏味道' },
    ],
    sidebar: [
      {
        text: '总览',
        items: [
          { text: '🏠 首页（章节摘要）', link: '/' },
          { text: '新人上手指南', link: '/RXAS400ADM-新人上手指南' },
          { text: '工具链正则避坑指南', link: '/RXAS400ADM-工具链正则避坑指南' },
        ],
      },
      {
        text: '系统流程图',
        items: [
          { text: '📐 架构总览', link: '/flowcharts/' },
          { text: '请求拦截器链', link: '/flowcharts/09-interceptors' },
          { text: '数据表与权限码总览', link: '/flowcharts/10-tables' },
          { text: '定时任务与后台采集', link: '/flowcharts/08-scheduler' },
          { text: '用户登录与认证', link: '/flowcharts/01-auth' },
        ],
      },
      {
        text: 'Dashboard',
        items: [
          { text: '总览', link: '/flowcharts/02-dashboard' },
        ],
      },
      {
        text: '监控中心',
        collapsed: true,
        items: [
          { text: '监控概览', link: '/flowcharts/04-monitor' },
          { text: '告警规则', link: '/flowcharts/04-monitor#alert-rules' },
          { text: '服务器对比', link: '/flowcharts/04-monitor#server-compare' },
          { text: '巡检报告', link: '/flowcharts/04-monitor#inspection' },
          { text: '监控指标', link: '/flowcharts/04-monitor#metrics' },
        ],
      },
      {
        text: '作业与任务',
        collapsed: true,
        items: [
          { text: 'Job 中心', link: '/flowcharts/03-job' },
          { text: '作业SLA', link: '/flowcharts/03-job#job-sla' },
          { text: '作业依赖图', link: '/flowcharts/03-job#job-sla' },
        ],
      },
      {
        text: '数据与对象',
        collapsed: true,
        items: [
          { text: '数据查询', link: '/flowcharts/05-query' },
          { text: '业务数据浏览', link: '/flowcharts/05-query#biz-data' },
          { text: '文件字段', link: '/flowcharts/05-query#biz-data' },
          { text: '消息文件', link: '/flowcharts/06-as400-tools#other-tools' },
          { text: '系统值', link: '/flowcharts/06-as400-tools#other-tools' },
          { text: '数据区域', link: '/flowcharts/06-as400-tools#other-tools' },
        ],
      },
      {
        text: 'AS400 运维',
        collapsed: true,
        items: [
          { text: '服务器管理', link: '/flowcharts/06-as400-tools#server-manage' },
          { text: '对象搜索', link: '/flowcharts/06-as400-tools#objects' },
          { text: 'IFS 文件', link: '/flowcharts/06-as400-tools#ifs' },
          { text: '作业调度', link: '/flowcharts/06-as400-tools#schedules' },
          { text: '命令脚本', link: '/flowcharts/06-as400-tools#scripts' },
          { text: '系统服务', link: '/flowcharts/06-as400-tools#subsystems' },
          { text: '执行审计', link: '/flowcharts/06-as400-tools#executions' },
          { text: 'PF 文件浏览', link: '/flowcharts/06-as400-tools#pf' },
          { text: '源代码', link: '/flowcharts/06-as400-tools#source' },
        ],
      },
      { text: '业务管理（BPCS）',
        collapsed: true,
        items: [
          { text: '订单时间轴', link: '/flowcharts/11-bpcs#order-timeline' },
          { text: '客户档案', link: '/flowcharts/11-bpcs#customer' },
          { text: '库存可用量', link: '/flowcharts/11-bpcs#inventory' },
          { text: '发运看板', link: '/flowcharts/11-bpcs#shipping' },
          { text: '发票轨迹', link: '/flowcharts/11-bpcs#invoice' },
          { text: '销售趋势', link: '/flowcharts/11-bpcs#sales' },
          { text: '采购订单', link: '/flowcharts/11-bpcs#purchase' },
          { text: '物料主档', link: '/flowcharts/11-bpcs#item' },
          { text: '订单列表', link: '/flowcharts/11-bpcs#sc-phase1' },
          { text: '库存预警', link: '/flowcharts/11-bpcs#sc-phase1' },
          { text: '销售分析', link: '/flowcharts/11-bpcs#sc-phase1' },
          { text: '库存变动', link: '/flowcharts/11-bpcs#sc-phase2' },
          { text: '采购收货', link: '/flowcharts/11-bpcs#sc-phase2' },
          { text: '发运列表', link: '/flowcharts/11-bpcs#sc-phase2' },
          { text: 'ABC 分析', link: '/flowcharts/11-bpcs#sc-phase3' },
          { text: '供应商绩效', link: '/flowcharts/11-bpcs#sc-phase3' },
          { text: '供应链 KPI', link: '/flowcharts/11-bpcs#sc-phase4' },
          { text: '订单全链路', link: '/flowcharts/11-bpcs#sc-phase4' },
        ],
      },
      {
        text: '系统管理',
        collapsed: true,
        items: [
          { text: '用户管理', link: '/flowcharts/07-system#users' },
          { text: '角色管理', link: '/flowcharts/07-system#roles' },
          { text: '菜单管理', link: '/flowcharts/07-system#menus' },
          { text: '权限码管理', link: '/flowcharts/07-system#permissions' },
          { text: '系统配置', link: '/flowcharts/07-system#config' },
          { text: '翻译管理', link: '/flowcharts/07-system#config' },
          { text: '字典管理', link: '/flowcharts/07-system#config' },
          { text: '登录日志', link: '/flowcharts/07-system#other' },
          { text: '缓存管理', link: '/flowcharts/07-system#other' },
          { text: '定时任务', link: '/flowcharts/07-system#other' },
          { text: 'IP 黑白名单', link: '/flowcharts/07-system#other' },
          { text: 'Webhook', link: '/flowcharts/07-system#other' },
          { text: '权限申请', link: '/flowcharts/07-system#other' },
          { text: '参数维护', link: '/flowcharts/07-system#other' },
          { text: '通知公告', link: '/flowcharts/07-system#other' },
          { text: '通知中心', link: '/flowcharts/07-system#other' },
        ],
      },
      { text: '报表与文档',
        collapsed: true,
        items: [
          { text: '报表中心', link: '/flowcharts/12-reports#report-gen' },
          { text: '文档管理', link: '/flowcharts/12-reports#doc-center' },
          { text: '知识库', link: '/flowcharts/12-reports#doc-center' },
          { text: '模块流程图', link: '/flowcharts/12-reports#doc-center' },
        ],
      },
      {
        text: '邮件中心',
        collapsed: true,
        items: [
          { text: '写邮件', link: '/flowcharts/13-email#email-compose' },
          { text: '邮件设置', link: '/flowcharts/13-email#email-settings' },
          { text: '收件人分组', link: '/flowcharts/13-email#email-groups' },
          { text: '邮件日志', link: '/flowcharts/13-email#email-logs' },
        ],
      },
      {
        text: '发布与代码 / 工具',
        collapsed: true,
        items: [
          { text: '审计日志', link: '/flowcharts/14-tools#tools' },
          { text: '健康巡检', link: '/flowcharts/14-tools#tools' },
          { text: '拓扑分析', link: '/flowcharts/14-tools#tools' },
          { text: '区域管理', link: '/flowcharts/14-tools#tools' },
          { text: '日历', link: '/flowcharts/14-tools#tools' },
          { text: '源代码', link: '/flowcharts/14-tools#release' },
          { text: '编译管理', link: '/flowcharts/14-tools#release' },
        ],
      },
      {
        text: '代码审计报告（按章拆页）',
        items: [
          { text: '一、Top 5 坏味道', link: '/review/trae/01-直击痛点阻碍新人理解的-top-5-核心坏味道' },
          { text: '二、类型安全与契约', link: '/review/trae/02-类型安全与契约新人理解的核心' },
          { text: '三、数据访问与业务分层', link: '/review/trae/03-数据访问与业务分层mybatis-plus-特性审计' },
          { text: '四、Vue 3 组合式 API', link: '/review/trae/04-vue-3-组合式-api-与状态流向' },
          { text: '五、可读性与坏味道', link: '/review/trae/05-可读性与坏味道排查' },
          { text: '六、架构亮点', link: '/review/trae/06-架构亮点值得保持' },
          { text: '七、30 天新人降本清单', link: '/review/trae/07-30-天新人降本清单' },
          { text: '八、补充审计', link: '/review/trae/08-补充审计架构代码质量冗余代码布局i18n' },
          { text: '九、综合评分', link: '/review/trae/09-综合评分' },
          { text: '十、深度架构审计', link: '/review/trae/10-深度架构审计entity-暴露越权n1事务安全' },
          { text: '十一、AS400/DB2 for i 专项', link: '/review/trae/11-as400db2-for-i-专项审计事务承诺控制journaling-与回滚安全' },
          { text: '十二、动态事务开关设计', link: '/review/trae/12-as400-动态事务开关架构设计nooptransactionmanager-与-sql7008-防治' },
          { text: '十三、CI/CD 多环境部署', link: '/review/trae/13-cicd-多环境自动化切换as400-部署流水线设计' },
          { text: '十四、响应式系统审计', link: '/review/trae/14-vue-3-组合式-api-深度审计refreactive-选型与响应式系统' },
          { text: '十五、i18n 深度对比', link: '/review/trae/15-i18n-国际化深度对比审计键值一致性缺失与硬编码排查' },
          { text: '十六、综合评分（更新）', link: '/review/trae/16-综合评分更新' },
          { text: '十七、补充优先级清单', link: '/review/trae/17-补充优先级清单扩展' },
          { text: '十八、优化建议（复核追加）', link: '/review/trae/18-优化建议2026-08-15-复核后追加' },
          { text: '十九、取消全部事务控制（最终决策）', link: '/review/trae/10-十九取消全部事务控制决策与执行方案2026-08-15-最终决策' },
          { text: '十九、优化-todo-清单（执行计划）', link: '/review/trae/10-十九优化-todo-清单2026-08-16-执行计划' },
          { text: '附录：审计数据统计', link: '/review/trae/99-附录-审计数据统计2026-08-15-复核修订' },
          { text: '附录：AAA-Remark 状态回填（2026-08-16）', link: '/review/trae/99-附录-aaa-remark-状态回填2026-08-16' },
        ],
      },
    ],
    search: { provider: 'local' },
    outline: { level: [2, 3] },
    docFooter: { prev: '上一篇', next: '下一篇' },
    lastUpdated: { text: '更新于' },
  },
})