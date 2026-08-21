# RXAS400ADM 全栈代码审计报告

> **审计日期**：2026-08-15
> **审计范围**：Backend (Spring Boot + MyBatis-Plus) + Frontend (Vue 3 + TypeScript)
> **核心痛点**：新人进组"看不懂逻辑、不敢改代码、找不到调用链"
> **评级标准**：[阻碍理解/Blocker] > [规范警告/Warning] > [重构建议/Info]

---

本页为**审计文档站首页**。Trae 审计报告按章拆页（见左侧「Trae 审计报告」），审计合订本保持单页全文（线性 review 日志，拆页反而不利导航），全文搜索覆盖站内所有页面。

## 章节摘要（快速定位）

| 章节 | 审计轮次 | 核心结论 | 相关 Action |
| --- | --- | --- | --- |
| 一 | 首轮 | Top 5 坏味道全部真实：Map 传输 / QueryWrapper / 类型断言 / CTE / 内联类型 | §7 Action 1-3 |
| 二 | 首轮 | request.ts 8 处 `as unknown as`、MenuService Map 黑盒、SysRole 重复定义 | Action 1-2 |
| 三 | 首轮 | QueryWrapper 212 处 / 43 文件、CTE 注解 SQL、0 个 XML | Action 3 |
| 四 | 首轮 | TagsView 手动同步 ×5、Users.vue setup 偏大、useTablePage 良好 | P2-1 |
| 五 | 首轮 | 魔法值散落、ApiResponse.data 缺 null、P-标记注释 | 随改动收敛 |
| 六 | 首轮 | useTablePage / CSS 工具类 / DTO-VO 三层隔离 = 亮点 | 保持 |
| 七 | 首轮 | 降本清单 Action 1-3（VO 化 / 类型断言 / XML） | Action 1-3 |
| 八 | 二轮 | Service 接口化不足、rollbackFor 缺失、硬编码中文、内联 style 17 处 | Action 4-6 |
| 九 | 二轮 | 初版评分（已被 §十六 取代） | — |
| 十 | 三轮 | Entity 暴露（P2-10 已 VO 化 8 个 Controller）、bindRoles N+1、安全全绿 | 存量收敛 |
| 十一 | 四轮 | rollbackFor 缺 55 处（**已闭环**：全库补齐 + check-transactional.sh 门禁）、Journaling 运维前置、长事务 3 处 | P0-P3 |
| 十二 | 五轮 | 动态事务开关 = 前瞻设计（当前 MySQL 主数据源，无需） | 暂缓 |
| 十三 | 六轮 | 构建/门禁完整，AS400 多环境部署缺失 | 暂缓 |
| 十四 | 七轮 | ref/reactive 选型规范，TagsView 同步与 §四同题 | P2-1 |
| 十五 | 八轮 | i18n key 全对称、翻译质量高、无硬编码中文（P2-30 已收敛残留） | 保持 |
| 十六 | 终版 | 类型安全 / 数据访问为最大短板（最终评分） | §十八 |
| 十七 | 终版 | P0-P2 排期（已按 MySQL 现实校准） | §十八 A-D |
| 十八 | 复核 | A-E 行动指引：立即实施 / 下迭代 / 门禁 / 暂缓 / 文档 | 执行 A 组 |
| 附录 | 复核 | 统计修正：212 / 79 / 8 处（原 50+ / 10+ / 4） | — |

## 站点导航

- **Trae 审计报告**：按章拆页（`docs/review/trae/`，由 `npm run split:trae` 从 `Trae-RXAS400ADM-2026-08-15.md` 重新生成）
- **审计合订本**：`CodeReview-RXAS400ADM-2026-08-14.md` 单页全文（轮次 0-12 + Action 1/2 + 归档说明）
- **新人上手指南**：`RXAS400ADM-新人上手指南.md`
- **工具链正则避坑指南**：`RXAS400ADM-工具链正则避坑指南.md`

## 本地预览

```bash
cd docs && npm install && npm run dev   # http://localhost:5174
```

> 说明：源文件修改后，`npm run split:trae` 重新生成章节页，再 `npm run build` 发布。