import { defineConfig } from 'vitepress'
import { withMermaid } from 'vitepress-plugin-mermaid'

/**
 * RXAS400ADM 审计文档站（VitePress）
 * - Trae 审计报告按章拆页（docs/review/trae/，由 scripts/split-trae.mjs 生成）
 * - 审计合订本保持单页全文（线性 review 日志，拆页反而不利导航；搜索仍全文覆盖）
 * - 全文搜索：provider local（内置 MiniSearch，无需外部服务）
 * - 主源文件（Trae 报告）已拆页，从站点排除（srcExclude）
 * - Mermaid：withMermaid 让 ```mermaid 代码块在页面内直接渲染（flowcharts/*.md）
 */
export default withMermaid(defineConfig({
  lang: 'zh-CN',
  title: 'RXAS400ADM 审计与文档',
  description: '代码审计报告 / 复核备注 / 新人上手 / 门禁指南（2026-08-15）',
  cleanUrls: true,
  // 站点只收录：首页 + Trae 拆页(review/trae) + 合订本 + 上手指南 + 避坑指南。
  // 其余历史遗留 md 已并入合订本或属过程文档，且部分含未闭合 HTML 标签（如 项目开发步骤追踪.md:663），
  // 全部排除以免构建失败 / 干扰搜索。
  srcExclude: [
    'Trae-RXAS400ADM-2026-08-15.md',
    'Trae-RXAS400ADM-2026-08-16.md',
    'RXAS400-vs-RXAS400ADM-对比分析.md',
    'RXAS400-分析总结.md',
    'RXAS400ADM项目全面代码审查报告.md',
    'US400CND部署操作手册.md',
    '代码质量审计.md',
    '修复优化清单-易实现高收益优先排序.md',
    '前端体验优化追踪.md',
    '前端布局样式分页对比借鉴分析.md',
    '已实现功能模块清单.md',
    '开发约定与Skills分析.md',
    '旧项目AS400功能借鉴清单.md',
    '旧项目前端可借鉴功能清单.md',
    '部署到US400CND.md',
    '项目开发步骤追踪.md',
    '项目架构与功能说明.md',
    'deep-research-report-20260817-A.md',
    'deep-research-report-20260817-B.md',
    'codereview-report-20260817.md',
    'db2i-migration-research-20260817.md',
  ],
  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      { text: 'Trae 审计（按章）', link: '/review/trae/01-直击痛点阻碍新人理解的-top-5-核心坏味道' },
      { text: '审计合订本', link: '/CodeReview-RXAS400ADM-2026-08-14' },
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
        text: 'Trae 审计报告（按章拆页）',
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
          { text: '附录：审计数据统计', link: '/review/trae/99-附录-审计数据统计2026-08-15-复核修订' },
          { text: '附录：AAA-Remark 状态回填（2026-08-16）', link: '/review/trae/99-附录-aaa-remark-状态回填2026-08-16' },
        ],
      },
      {
        text: '审计合订本（单页全文）',
        items: [
          { text: 'CodeReview-RXAS400ADM-2026-08-14', link: '/CodeReview-RXAS400ADM-2026-08-14' },
        ],
      },
    ],
    search: { provider: 'local' },
    outline: { level: [2, 3] },
    docFooter: { prev: '上一篇', next: '下一篇' },
    lastUpdated: { text: '更新于' },
  },
}))
