import { describe, expect, it } from 'vitest'
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { dirname, join, relative } from 'node:path'
import { spawnSync } from 'node:child_process'
import { findJoinedTemplateSlots } from '../../scripts/check-template-join.mjs'
import {
  detectDynamicClassKeys,
  extractTemplateClasses,
  findUndefinedTemplateClasses,
} from '../../scripts/check-template-classes.mjs'
import { findI18nPrefixMissingAdd } from '../../scripts/check-i18n.mjs'
import { readFileSync } from 'node:fs'

// npm test 恒在 frontend/ 下执行（package.json script），cwd 即 frontend 根
const FRONTEND = process.cwd()
const REPO = dirname(FRONTEND)

/** 检测当前环境是否有可用的 bash */
function getBashPath(): string | null {
  const candidates = [
    'bash',
    'C:\\Program Files\\Git\\bin\\bash.exe',
    'C:\\Program Files (x86)\\Git\\bin\\bash.exe',
    '/usr/bin/bash',
    '/bin/bash',
  ]
  for (const candidate of candidates) {
    const r = spawnSync(candidate, ['-c', 'echo ok'], { encoding: 'utf8', timeout: 3000 })
    if (r.status === 0 && r.stdout.trim() === 'ok') {
      return candidate
    }
  }
  return null
}

const BASH_PATH = getBashPath()
const BASH_AVAILABLE = BASH_PATH !== null

/** 仅当 bash 可用时运行测试组的 describe 包装 */
const describeIfBash = BASH_AVAILABLE ? describe : describe.skip

/** 在 frontend/ 下建临时 fixture 目录（相对仓库根传入 bash 门禁），返回 { abs, rel } */
function fixture(files: Record<string, string>) {
  const abs = mkdtempSync(join(FRONTEND, '.tmp-gate-tests-'))
  for (const [name, content] of Object.entries(files)) {
    writeFileSync(join(abs, name), content)
  }
  return { abs, rel: relative(REPO, abs).replace(/\\/g, '/') }
}

function cleanup(abs: string) {
  rmSync(abs, { recursive: true, force: true })
}

/** 跑 bash 门禁（R1/R2），返回退出码与输出。bash 不可用时返回 { code: -1, out: 'BASH_UNAVAILABLE' } */
function runSlotGate(rel: string) {
  if (!BASH_AVAILABLE) return { code: -1, out: 'BASH_UNAVAILABLE' }
  const res = spawnSync(BASH_PATH!, ['scripts/check-frontend-slots.sh', rel], { cwd: REPO, encoding: 'utf8' })
  return { code: res.status, out: `${res.stdout}\n${res.stderr}` }
}

const OK_FILE = `<template>
  <el-table :data="rows" size="small">
    <el-table-column>
      <template #default="{ row }: { row: UserVO }">
        <span>{{ row.name }}</span>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { UserVO } from '@/api/user'
const rows = ref<UserVO[]>([])
</script>
`

describe('check-template-join.mjs: findJoinedTemplateSlots', () => {
  it('检出「开标签 + <template #default>」拼行（LF）', () => {
    const content = `<el-table :data="rows">
  <el-table-column prop="x" width="100">            <template #default="{ row }: { row: X }">
    <span>x</span>
  </el-table-column>
</el-table>
`
    const hits = findJoinedTemplateSlots(content)
    expect(hits).toHaveLength(1)
    expect(hits[0].line).toBe(2)
  })

  it('检出拼行（CRLF 内容同样按行识别）', () => {
    const content = '  <el-table-column prop="x" width="100">            <template #default="{ data }">\r\n'
    const hits = findJoinedTemplateSlots(content)
    expect(hits).toHaveLength(1)
  })

  it('正常多行（开标签与 template 各占一行）不误报', () => {
    const content = `<el-table-column prop="x" :label="y">
  <template #default="{ row }: { row: X }">
    <span>{{ row.x }}</span>
  </template>
</el-table-column>
`
    expect(findJoinedTemplateSlots(content)).toHaveLength(0)
  })

  it('单独一行的 <template> 不误报', () => {
    const content = `  <template #default="{ row }: { row: X }">
    <span>{{ row.x }}</span>
  </template>
`
    expect(findJoinedTemplateSlots(content)).toHaveLength(0)
  })
})

describeIfBash('check-frontend-slots.sh R1: 裸作用域插槽即失败', () => {
  it('全类型化文件通过', () => {
    const f = fixture({ 'ok.vue': OK_FILE })
    try {
      const r = runSlotGate(f.rel)
      expect(r.code).toBe(0)
    } finally {
      cleanup(f.abs)
    }
  })

  it('裸 `{ row }` 告警但不阻断（warning 模式）', () => {
    const f = fixture({ 'bare.vue': OK_FILE.replace('#default="{ row }: { row: UserVO }"', '#default="{ row }"') })
    try {
      const r = runSlotGate(f.rel)
      expect(r.code).toBe(0)
      // warning 模式仅显示计数，不列文件名
    } finally {
      cleanup(f.abs)
    }
  })

  it('裸 `{ data }`（树插槽）告警但不阻断', () => {
    const f = fixture({ 'tree.vue': '<template><el-tree :data="nodes"><template #default="{ data }"><span>{{ data.name }}</span></template></el-tree></template>\n' })
    try {
      const r = runSlotGate(f.rel)
      expect(r.code).toBe(0)
    } finally {
      cleanup(f.abs)
    }
  })

  it('裸 `{ item }`（通用解构，如 el-select）告警但不阻断', () => {
    const f = fixture({ 'sel.vue': '<template><el-select><template #default="{ item }"><span>{{ item }}</span></template></el-select></template>\n' })
    try {
      const r = runSlotGate(f.rel)
      expect(r.code).toBe(0)
    } finally {
      cleanup(f.abs)
    }
  })

  it('裸 `{ node, data }` 变体告警但不阻断', () => {
    const f = fixture({ 'tree2.vue': '<template><el-tree><template #default="{ node, data }"><span>{{ node.label }}</span></template></el-tree></template>\n' })
    try {
      const r = runSlotGate(f.rel)
      expect(r.code).toBe(0)
    } finally {
      cleanup(f.abs)
    }
  })

  it('无作用域的内容插槽（<template #default>纯内容</template>）不误报', () => {
    const f = fixture({
      'content.vue': '<template><el-dialog><template #default><p>hello</p></template></el-dialog></template>\n',
    })
    try {
      const r = runSlotGate(f.rel)
      expect(r.code).toBe(0)
    } finally {
      cleanup(f.abs)
    }
  })
})

describeIfBash('check-frontend-slots.sh R2: 标注的 RowType 必须已 import 或本地声明', () => {
  it('`import type { X }` 语句导入 → 通过', () => {
    const f = fixture({ 'ok.vue': OK_FILE })
    try {
      expect(runSlotGate(f.rel).code).toBe(0)
    } finally {
      cleanup(f.abs)
    }
  })

  it('`import { type X }` 内联 type 导入 → 通过', () => {
    const f = fixture({
      'ok2.vue': OK_FILE.replace("import type { UserVO } from '@/api/user'", "import { ref, type UserVO } from 'vue'"),
    })
    try {
      expect(runSlotGate(f.rel).code).toBe(0)
    } finally {
      cleanup(f.abs)
    }
  })

  it('本地 interface 声明 → 通过', () => {
    const f = fixture({
      'local.vue': OK_FILE.replace("import type { UserVO } from '@/api/user'\n", '').replace('UserVO', 'LocalRow').replace(
        /interface [\s\S]*?$/,
        '',
      ) + '\ninterface LocalRow { name: string }\n',
    })
    try {
      expect(runSlotGate(f.rel).code).toBe(0)
    } finally {
      cleanup(f.abs)
    }
  })

  it('内联匿名对象类型 `{ key: string }` → 通过', () => {
    const f = fixture({
      'inline.vue': '<template><el-table :data="rows"><el-table-column><template #default="{ row }: { row: { key: string } }"><span>{{ row.key }}</span></template></el-table-column></el-table></template>\n<script setup lang="ts">\nconst rows = [{ key: \'a\' }]\n</script>\n',
    })
    try {
      expect(runSlotGate(f.rel).code).toBe(0)
    } finally {
      cleanup(f.abs)
    }
  })

  it('悬空类型（未 import/声明）→ 失败', () => {
    const f = fixture({
      'dangling.vue': OK_FILE.replace("import type { UserVO } from '@/api/user'\n", ''),
    })
    try {
      const r = runSlotGate(f.rel)
      expect(r.code).toBe(1)
      expect(r.out).toContain('UserVO')
    } finally {
      cleanup(f.abs)
    }
  })
})

// ---------------- check-layering.sh（backend 分层准绳） ----------------

/** 建临时 backend 目录（含 *Controller.java fixture），返回 { abs, rel } */
function layerFixture(files: Record<string, string>) {
  const abs = mkdtempSync(join(REPO, '.tmp-layer-tests-'))
  for (const [name, content] of Object.entries(files)) {
    writeFileSync(join(abs, name), content)
  }
  return { abs, rel: relative(REPO, abs).replace(/\\/g, '/') }
}

/** 跑 bash 分层门禁，返回退出码与输出（--strict 可选）。bash 不可用时返回 { code: -1, out: 'BASH_UNAVAILABLE' } */
function runLayerGate(rel: string, strict = false) {
  if (!BASH_AVAILABLE) return { code: -1, out: 'BASH_UNAVAILABLE' }
  const args = strict ? ['--strict', rel] : [rel]
  const res = spawnSync(BASH_PATH!, ['scripts/check-layering.sh', ...args], { cwd: REPO, encoding: 'utf8' })
  return { code: res.status, out: `${res.stdout}\n${res.stderr}` }
}

const CLEAN_CTRL = `package com.example;

@RestController
public class CleanController {
  public ApiResponse<UserVO> list() { return null; }
  public ApiResponse<String> save(@RequestBody UserDTO dto) { return null; }
}
`

function layerCtrl(name: string, body: string) {
  return `package com.example;\n\n@RestController\npublic class ${name} {\n${body}\n}\n`
}

describeIfBash('check-layering.sh R1: Controller 禁 new QueryWrapper / 注入 Mapper', () => {
  it('干净 Controller（DTO 入参 + VO 返回）通过', () => {
    const f = layerFixture({ 'CleanController.java': CLEAN_CTRL })
    try {
      expect(runLayerGate(f.rel).code).toBe(0)
    } finally {
      cleanup(f.abs)
    }
  })

  it('新增文件 new QueryWrapper → 失败', () => {
    const f = layerFixture({
      'NewController.java': layerCtrl('NewController', '  public Object x() { new QueryWrapper<>(); return null; }\n'),
    })
    try {
      const r = runLayerGate(f.rel)
      expect(r.code).toBe(1)
      expect(r.out).toContain('NewController')
    } finally {
      cleanup(f.abs)
    }
  })

  it('新增文件注入 Mapper → 失败', () => {
    const f = layerFixture({
      'NewController.java': layerCtrl('NewController', '  private final XxxMapper mapper;\n'),
    })
    try {
      expect(runLayerGate(f.rel).code).toBe(1)
    } finally {
      cleanup(f.abs)
    }
  })

  it('历史遗留文件（HealthController）也已下沉 Service，直拼 Wrapper 即失败（R1 白名单清零）', () => {
    const f = layerFixture({
      'HealthController.java': layerCtrl('HealthController', '  public Object x() { new QueryWrapper<>(); return null; }\n'),
    })
    try {
      const r = runLayerGate(f.rel)
      expect(r.code).toBe(1)
      expect(r.out).toContain('HealthController')
    } finally {
      cleanup(f.abs)
    }
  })
})

describeIfBash('check-layering.sh R2/R3: @RequestBody 必须 DTO、返回禁直返 Entity', () => {
  it('@RequestBody 新 Entity（非 DTO/白名单）→ 失败', () => {
    const f = layerFixture({
      'NewController.java': layerCtrl('NewController', '  public ApiResponse<String> save(@RequestBody SomeNewEntity e) { return null; }\n'),
    })
    try {
      const r = runLayerGate(f.rel)
      expect(r.code).toBe(1)
      expect(r.out).toContain('SomeNewEntity')
    } finally {
      cleanup(f.abs)
    }
  })

  it('历史遗留 Entity（ReportSchedule）也已 DTO 化，入参即失败（R2 白名单清零）', () => {
    const f = layerFixture({
      'SomeController.java': layerCtrl(
        'SomeController',
        '  public ApiResponse<String> save(@RequestBody ReportSchedule e) { return null; }\n',
      ),
    })
    try {
      const r = runLayerGate(f.rel)
      expect(r.code).toBe(1)
      expect(r.out).toContain('ReportSchedule')
    } finally {
      cleanup(f.abs)
    }
  })

  it('ApiResponse<新 Entity>（直返 Entity）→ 失败', () => {
    const f = layerFixture({
      'NewController.java': layerCtrl('NewController', '  public ApiResponse<SomeNewEntity> get() { return null; }\n'),
    })
    try {
      const r = runLayerGate(f.rel)
      expect(r.code).toBe(1)
      expect(r.out).toContain('SomeNewEntity')
    } finally {
      cleanup(f.abs)
    }
  })

  it('返回全新 *VO（未登记白名单）→ 通过（R3 加固，免维护白名单）', () => {
    const f = layerFixture({
      'NewController.java': layerCtrl('NewController', '  public ApiResponse<BrandNewVO> get() { return null; }\n'),
    })
    try {
      const r = runLayerGate(f.rel)
      expect(r.code).toBe(0)
    } finally {
      cleanup(f.abs)
    }
  })

  it('返回 *VO 但入参为全新 Entity → R2 仍拦截（R3 不误放 R2）', () => {
    const f = layerFixture({
      'NewController.java': layerCtrl(
        'NewController',
        '  public ApiResponse<BrandNewVO> save(@RequestBody SomeNewEntity e) { return null; }\n',
      ),
    })
    try {
      const r = runLayerGate(f.rel)
      expect(r.code).toBe(1)
      expect(r.out).toContain('SomeNewEntity')
    } finally {
      cleanup(f.abs)
    }
  })
})

describe('check-v38-consistency.mjs: V38 种子 ↔ V38_EXPECT_* 静态一致', () => {
  it('当前 V38 SQL 与 verify-fresh-db.sh 期望值一致（无需 MySQL）', () => {
    const res = spawnSync('node', ['scripts/check-v38-consistency.mjs'], { cwd: REPO, encoding: 'utf8' })
    expect(res.status).toBe(0)
  })
})

describe('check-migrations.mjs: 迁移文件结构一致性（V1~V42+）', () => {
  it('当前迁移目录通过序列/命名/对象去重/空迁移/MANIFEST 校验（无需 MySQL）', () => {
    const res = spawnSync('node', ['scripts/check-migrations.mjs'], { cwd: REPO, encoding: 'utf8' })
    expect(res.status).toBe(0)
  })

  it('V42 文档化对象断言对象存在（MANIFEST 覆盖最新迁移）', () => {
    const res = spawnSync('node', ['scripts/check-migrations.mjs'], { cwd: REPO, encoding: 'utf8' })
    expect(res.stdout).toContain('idx_metric_instance_time')
    expect(res.stdout).toContain('迁移结构一致性通过')
    expect(res.status).toBe(0)
  })
})

describe('check-template-classes.mjs: 模板 class 必须有样式定义（icon-picker 类回归）', () => {
  it('extractTemplateClasses 只提取 <template> 段（排除 script 字符串干扰）', () => {
    const content = `<script setup lang="ts">
const cls = 'script-string-class'
</script>
<template>
  <div class="page-container"><span class="table-wrapper x">t</span></div>
</template>
<style scoped>
.table-wrapper {}
</style>
`
    const classes = extractTemplateClasses(content)
    expect(classes.has('page-container')).toBe(true)
    expect(classes.has('table-wrapper')).toBe(true)
    expect(classes.has('script-string-class')).toBe(false)
  })

  it('未定义 class → 检出；scoped/全局定义或白名单前缀 → 放行', () => {
    const f = fixture({
      'Bad.vue': `<template>
  <div class="ghost-panel">x</div>
</template>
`,
      'Good.vue': `<template>
  <div class="styled-panel">x</div>
</template>
<style scoped>
.styled-panel {}
</style>
`,
      'Global.vue': `<template>
  <div class="global-panel">x</div>
</template>
`,
      'Whitelisted.vue': `<template>
  <el-button class="fa-solid fa-gear" />
</template>
`,
      'common.css': `.global-panel {}
`,
    })
    try {
      const vueFiles = [
        join(f.abs, 'Bad.vue'),
        join(f.abs, 'Good.vue'),
        join(f.abs, 'Global.vue'),
        join(f.abs, 'Whitelisted.vue'),
      ]
      const bad = findUndefinedTemplateClasses(vueFiles, [join(f.abs, 'common.css')])
      expect(bad.map((b: { className: string }) => b.className)).toEqual(['ghost-panel'])
    } finally {
      cleanup(f.abs)
    }
  })

  it('icon-picker 网格类（曾因样式丢失竖排）已在 common.css 定义，全量扫描 0 遗漏', () => {
    const res = spawnSync('node', ['scripts/check-template-classes.mjs'], { cwd: FRONTEND, encoding: 'utf8' })
    expect(res.status).toBe(0)
    expect(res.stdout).toContain('全部有')
  })

  it(':style 内联样式中的字符串不提取为 class（v2 误报防御）', () => {
    const content = `<template>
  <div :style="'ghost-class'" :class="{ active: on }">x</div>
</template>
`
    const classes = extractTemplateClasses(content)
    expect(classes.has('ghost-class')).toBe(false)
    expect(classes.has('active')).toBe(true)
  })

  it('子组件嵌套元素使用仅定义于父组件 scoped 的类 → 检出（v2 漏报修复：父 scoped 到不了子内部）', () => {
    const f = fixture({
      'Parent.vue': `<script setup lang="ts">
import Child from './Child.vue'
</script>
<template>
  <Child />
</template>
<style scoped>
.private-style {}
</style>
`,
      'Child.vue': `<template>
  <div class="child-root">
    <span class="private-style">x</span>
  </div>
</template>
<style scoped>
.child-root {}
</style>
`,
    })
    try {
      const vueFiles = [join(f.abs, 'Parent.vue'), join(f.abs, 'Child.vue')]
      const bad = findUndefinedTemplateClasses(vueFiles, [], f.abs)
      expect(bad.map((b: { className: string }) => b.className)).toEqual(['private-style'])
    } finally {
      cleanup(f.abs)
    }
  })

  it('子组件根元素使用父组件 scoped 类 → 放行（v2 子根豁免）', () => {
    const f = fixture({
      'Parent.vue': `<script setup lang="ts">
import Child from './Child.vue'
</script>
<template>
  <Child />
</template>
<style scoped>
.root-style {}
</style>
`,
      'Child.vue': `<template>
  <div class="root-style">x</div>
</template>
`,
    })
    try {
      const vueFiles = [join(f.abs, 'Parent.vue'), join(f.abs, 'Child.vue')]
      const bad = findUndefinedTemplateClasses(vueFiles, [], f.abs)
      expect(bad).toEqual([])
    } finally {
      cleanup(f.abs)
    }
  })

  it('未标 scoped 的全局 style 块 → 任意组件可用（v2）', () => {
    const f = fixture({
      'A.vue': `<template>
  <div class="shared-global">x</div>
</template>
<style>
.shared-global {}
</style>
`,
      'B.vue': `<template>
  <div class="shared-global">x</div>
</template>
`,
    })
    try {
      const vueFiles = [join(f.abs, 'A.vue'), join(f.abs, 'B.vue')]
      const bad = findUndefinedTemplateClasses(vueFiles, [], f.abs)
      expect(bad).toEqual([])
    } finally {
      cleanup(f.abs)
    }
  })

  it('动态计算键 [dynamicClass] → 不误抓变量名，字面量分支仍提取（v2 动态 key 分析）', () => {
    const content = `<template>
  <div :class="{ [dynamicClass]: true, [flag ? 'expanded' : 'collapsed']: true }">t</div>
</template>
`
    const classes = extractTemplateClasses(content)
    expect(classes.has('dynamicClass')).toBe(false)
    expect(classes.has('expanded')).toBe(true)
    expect(classes.has('collapsed')).toBe(true)
    const dyn = detectDynamicClassKeys(content)
    expect(dyn.length).toBe(2)
    expect(dyn.map((d) => d.line)).toEqual([2, 2])
  })

  it('静态对象键与 :style 并存 → 不误报（v2）', () => {
    const content = `<template>
  <div class="theme-color-dot" :style="{ background: opt.color }">t</div>
</template>
`
    const classes = extractTemplateClasses(content)
    expect(classes.has('theme-color-dot')).toBe(true)
    expect(classes.has('background')).toBe(false)
    expect(classes.has('opt')).toBe(false)
  })

  it(':slotted()/::v-slotted() 穿透类 → 视为全局，插槽内容任意位置可用（v2.1）', () => {
    const f = fixture({
      'Parent.vue': `<template>
  <Child />
</template>
<style scoped>
::v-slotted(.slot-foo) {}
:slotted(.slot-bar) {}
</style>
`,
      'Child.vue': `<template>
  <div class="child-root">
    <span class="slot-foo">x</span>
    <span class="slot-bar">y</span>
  </div>
</template>
<style scoped>
.child-root {}
</style>
`,
    })
    try {
      const vueFiles = [join(f.abs, 'Parent.vue'), join(f.abs, 'Child.vue')]
      const bad = findUndefinedTemplateClasses(vueFiles, [], f.abs)
      expect(bad).toEqual([])
    } finally {
      cleanup(f.abs)
    }
  })

  it('SCSS 嵌套/@mixin 内类不算定义，@media 条件包装内算（v2.1 extractTopLevelClasses）', () => {
    const f = fixture({
      'Nested.vue': `<template>
  <div class="top-level">
    <span class="nested-only">x</span>
    <span class="media-class">y</span>
  </div>
</template>
<style scoped>
.top-level {}
.top-level .nested-only {
  color: red;
}
@media (max-width: 768px) {
  .media-class { color: blue; }
}
</style>
`,
    })
    try {
      const vueFiles = [join(f.abs, 'Nested.vue')]
      const bad = findUndefinedTemplateClasses(vueFiles, [], f.abs)
      // .nested-only 通过「后代选择器 .top-level .nested-only」定义（合法），.media-class 通过 @media 内定义
      expect(bad).toEqual([])
    } finally {
      cleanup(f.abs)
    }
  })

  it('属性值中的小数（0.5px）不干扰类提取与花括号栈（v2.1 锚定修复回归）', () => {
    const f = fixture({
      'Decimal.vue': `<template>
  <div class="first"><span class="second">x</span></div>
</template>
<style scoped>
.first {}
.first {
  letter-spacing: 0.5px;
}
.second { color: red; }
</style>
`,
    })
    try {
      const vueFiles = [join(f.abs, 'Decimal.vue')]
      const bad = findUndefinedTemplateClasses(vueFiles, [], f.abs)
      expect(bad).toEqual([])
    } finally {
      cleanup(f.abs)
    }
  })

  it(':deep() 穿透类 → 视为全局，子组件任意位置可用（v2）', () => {
    const f = fixture({
      'Parent.vue': `<script setup lang="ts">
import Child from './Child.vue'
</script>
<template>
  <Child />
</template>
<style scoped>
:deep(.pierced) {}
</style>
`,
      'Child.vue': `<template>
  <div class="child-root">
    <span class="pierced">x</span>
  </div>
</template>
<style scoped>
.child-root {}
</style>
`,
    })
    try {
      const vueFiles = [join(f.abs, 'Parent.vue'), join(f.abs, 'Child.vue')]
      const bad = findUndefinedTemplateClasses(vueFiles, [], f.abs)
      expect(bad).toEqual([])
    } finally {
      cleanup(f.abs)
    }
  })
})

describe('check-i18n.mjs M7: useFormDialog i18nPrefix 命名空间必须含 add 键', () => {
  it('命名空间缺 add → 检出（permissions.add 曾缺失）', () => {
    const f = fixture({ 'useFormDialog-holder.ts': `useFormDialog({ i18nPrefix: 'permissions' })\nuseFormDialog({ i18nPrefix: 'role' })\n` })
    try {
      const files = [join(f.abs, 'useFormDialog-holder.ts')]
      const zhKeys = new Set(['permissions.create', 'role.add'])
      const enKeys = new Set(['permissions.create', 'role.add'])
      const bad = findI18nPrefixMissingAdd(files, zhKeys, enKeys)
      expect(bad).toHaveLength(1)
      expect(bad[0].ns).toBe('permissions')
    } finally {
      cleanup(f.abs)
    }
  })

  it('所有命名空间均含 add（zh+en）→ 空结果', () => {
    const f = fixture({ 'useFormDialog-holder.ts': `useFormDialog({ i18nPrefix: 'permissions' })\nuseFormDialog({ i18nPrefix: 'role' })\n` })
    try {
      const files = [join(f.abs, 'useFormDialog-holder.ts')]
      const zhKeys = new Set(['permissions.add', 'role.add'])
      const enKeys = new Set(['permissions.add', 'role.add'])
      expect(findI18nPrefixMissingAdd(files, zhKeys, enKeys)).toHaveLength(0)
    } finally {
      cleanup(f.abs)
    }
  })

  it('en 缺 add 同样检出（防只改一边）', () => {
    const f = fixture({ 'useFormDialog-holder.ts': `useFormDialog({ i18nPrefix: 'permissions' })\n` })
    try {
      const files = [join(f.abs, 'useFormDialog-holder.ts')]
      const zhKeys = new Set(['permissions.add'])
      const enKeys = new Set(['permissions.create'])
      const bad = findI18nPrefixMissingAdd(files, zhKeys, enKeys)
      expect(bad).toHaveLength(1)
      expect(bad[0].ns).toBe('permissions')
    } finally {
      cleanup(f.abs)
    }
  })

  it('当前源码全量通过（含新增 add 键后的 permissions）', () => {
    const res = spawnSync('node', ['scripts/check-i18n.mjs'], { cwd: FRONTEND, encoding: 'utf8' })
    expect(res.status).toBe(0)
    expect(res.stdout).toContain('i18nPrefix 命名空间均含 add 键')
  })
})

describe('菜单管理页：禁止 default-expand-all（打开默认收起，防回归）', () => {
  it('menus/index.vue 不含 default-expand-all', () => {
    const content = readFileSync(join(FRONTEND, 'src/views/system/menus/index.vue'), 'utf-8')
    expect(content).not.toMatch(/default-expand-all/)
  })
})