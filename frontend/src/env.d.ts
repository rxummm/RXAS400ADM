/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  // eslint-disable-next-line @typescript-eslint/no-explicit-any -- Vue SFC 模块 shim 惯例写法
  const component: DefineComponent<{}, {}, any>
  export default component
}
