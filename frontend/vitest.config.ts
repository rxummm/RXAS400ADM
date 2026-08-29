import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

/**
 * vitest 单元测试配置（npm test）。
 * 与 vite.config.ts 共享 @ 别名；独立文件避免污染 dev/build 配置。
 * 测试范围：纯逻辑（useTablePage 缓存、canSeeTab 权限判断、密码策略），不启动 dev server。
 */
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'happy-dom',
    include: ['src/**/*.test.ts', 'src/**/*.spec.ts'],
  },
})
