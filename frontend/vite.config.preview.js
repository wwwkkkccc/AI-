import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  base: '/resume-ai/',
  server: {
    port: 5173
  },
  // 使用预览入口
  build: {
    rollupOptions: {
      input: {
        main: path.resolve(__dirname, 'index-preview.html')
      }
    }
  }
})
