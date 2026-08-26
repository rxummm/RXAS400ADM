/**
 * Element Plus 类型增强：让 el-table 的 #default 插槽支持泛型行类型。
 *
 * 问题：Element Plus 的 el-table 插槽类型定义使用 DefaultRow = Record<string, any>，
 * 但 vue-tsc 会将我们标注的 { row: SpecificType } 与 DefaultRow 做兼容性检查，
 * 导致 159 个 TS2322 错误（Type 'DefaultRow' is missing properties from type 'XxxType'）。
 *
 * 修复：重声明 TableColumnCtx 的 $slots 类型，使 row 接受任意类型。
 * 这样 <template #default="{ row }: { row: IbmiSystem }"> 中的类型标注
 * 仅作为开发时的类型提示，不会触发编译错误。
 *
 * 注：本文件是对第三方库内部类型的 shim，必须与 EP 源码的 any 签名保持一致，
 * 改为 unknown 会重新触发 TS2322，故此处豁免 no-explicit-any（全库唯一例外）。
 */
/* eslint-disable @typescript-eslint/no-explicit-any */

declare module 'element-plus' {
  interface TableColumnCtx<T = any> {
    $slots: {
      default?: (scope: { row: T; column: TableColumnCtx<T>; $index: number }) => any
      header?: (scope: { column: TableColumnCtx<T>; $index: number }) => any
    }
  }
}

export {}
