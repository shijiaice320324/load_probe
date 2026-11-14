import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发时把 /api 代理到本机后端，避免 CORS；生产部署时前端与后端同源，同样无需跨域配置
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        // 后端已启用 HTTPS（自签证书），secure:false 只影响开发代理
        target: 'https://127.0.0.1:8443',
        secure: false,
        changeOrigin: false
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1200
  }
})
