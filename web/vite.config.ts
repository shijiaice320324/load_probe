import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发时把 /api 代理到本机后端，避免 CORS；生产部署时前端与后端同源，同样无需跨域配置
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: false
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1200
  }
})
