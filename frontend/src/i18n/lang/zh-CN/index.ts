import common from './common'
import menu from './menu'
import dashboard from './dashboard'
import monitor from './monitor'
import jobs from './jobs'
import data from './data'
import assets from './assets'
import system from './system'
import docs from './docs'
import bpcs from './bpcs'
import opTemplate from './opTemplate'
import sysDoc from './sysDoc'
import network from './network'

export default {
  // 2026-08-16 修复（模块拆分盲区）：$t() 引用一律按「文件内顶层命名空间」书写
  // （如 $t('login.title') / $t('menu.dashboard') / $t('reports.xxx') / $t('users.xxx')），
  // 因此必须把各模块文件的顶层命名空间展开到根。模块名本体保留（$t('common.xxx') 等带前缀引用亦可用）；
  // 展开放在模块名之后；common/menu/dashboard/monitor/jobs/assets/docs 的文件内自带同名包装节，
  // 仅保留展开（避免 TS2783 重复键）；data/system 无包装节，保留模块名以支持 data.*/system.* 带前缀引用。
  data, system,
  ...common,
  ...menu,
  ...dashboard,
  ...monitor,
  ...jobs,
  ...data,
  ...assets,
  ...system,
  ...docs,
  ...opTemplate,
  ...bpcs,
  ...sysDoc,
  ...network,
}