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
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-element-plus': ['element-plus', '@element-plus/icons-vue'],
            'vendor-echarts': ['echarts/core', 'echarts/charts', 'echarts/components', 'echarts/renderers'],
            'vendor-md-editor': ['md-editor-v3', 'marked', 'dompurify'],
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
        '/ws': {
          target: wsTarget,
          ws: true,
          changeOrigin: true,
        },
      },
    },
  }
})
