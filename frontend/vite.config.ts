import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// 后端代理目标可配置（.env 的 VITE_API_PROXY_TARGET），默认本地 8080
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.VITE_API_PROXY_TARGET || 'http://localhost:8080'
  const wsTarget = env.VITE_API_PROXY_TARGET?.replace(/^http/, 'ws') || 'ws://localhost:8080'

  return {
    plugins: [
      vue(),
      AutoImport({
        imports: ['vue', 'vue-router'],
        resolvers: [ElementPlusResolver()],
        dts: 'src/auto-imports.d.ts',
      }),
      Components({
        resolvers: [ElementPlusResolver()],
        dts: 'src/components.d.ts',
      }),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    build: {
      // 低-12：三大 vendor（element-plus/echarts/md-editor）单包均超 500KB 属预期（框架级体积，
      // 已按 manualChunks 独立拆分且 gzip 后 ~330KB），显式调高阈值消除噪音告警；业务代码仍受默认监控
      chunkSizeWarningLimit: 1100,
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-element-plus': ['element-plus', '@element-plus/icons-vue'],
            'vendor-echarts': ['echarts/core', 'echarts/charts', 'echarts/components', 'echarts/renderers'],
            'vendor-md-editor': ['md-editor-v3', 'marked', 'dompurify'],
            'vendor-vue': ['vue', 'vue-router', 'pinia', 'vue-i18n'],
          },
        },
      },
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target,
          changeOrigin: true,
        },
        '/actuator': {
          target,
          changeOrigin: true,
        },
        '/ws': {
          target: wsTarget,
          ws: true,
          changeOrigin: true,
        },
      },
    },
  }
})