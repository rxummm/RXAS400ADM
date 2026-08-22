/**
 * Element Plus 类型增强：让 el-table 的 #default 插槽支持泛型行类型。
 *
 * 问题：Element Plus 的 el-table 插槽类型定义使用 DefaultRow = Record<string, any>，
 * 但 vue-tsc 会将我们标注的 { row: SpecificType } 与 DefaultRow 做兼容性检查，
 * 导致 159 个 TS2322 错误（Type 'DefaultRow' is missing properties from type 'XxxType'）。
 *
 * 修复：重声明 TableColumnCtx 的 $slots 类型，使 row 接受 any。
 * 这样 <template #default="{ row }: { row: IbmiSystem }"> 中的类型标注
 * 仅作为开发时的类型提示，不会触发编译错误。
 */
import type { TableColumnCtx } from 'element-plus'

declare module 'element-plus' {
  interface TableColumnCtx<T = any> {
    $slots: {
      default?: (scope: { row: T; column: TableColumnCtx<T>; $index: number }) => any
      header?: (scope: { column: TableColumnCtx<T>; $index: number }) => any
    }
  }
}

export {}
