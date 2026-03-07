#!/bin/bash

# 液态玻璃样式系统 - 快速预览脚本

echo "🎨 iOS 26 液态玻璃风格系统"
echo "================================"
echo ""

# 检查是否在正确的目录
if [ ! -d "frontend" ]; then
    echo "❌ 错误：请在项目根目录运行此脚本"
    exit 1
fi

cd frontend

# 检查依赖
echo "📦 检查依赖..."
if ! command -v npm &> /dev/null; then
    echo "❌ 错误：未安装 npm"
    exit 1
fi

# 安装 sass（如果未安装）
if ! npm list sass &> /dev/null; then
    echo "📥 安装 sass..."
    npm install -D sass
fi

# 创建临时预览文件
echo "🔧 创建预览配置..."

# 修改 main.js 临时导入 LiquidGlassDemo
cat > src/main-preview.js << 'EOF'
import { createApp } from 'vue'
import LiquidGlassDemo from './components/LiquidGlassDemo.vue'
import './styles/liquid-glass.scss'

createApp(LiquidGlassDemo).mount('#app')
EOF

# 修改 vite.config.js 使用预览入口
if [ -f "vite.config.js" ]; then
    cp vite.config.js vite.config.js.backup
fi

cat > vite.config.preview.js << 'EOF'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    port: 5174,
    open: true
  }
})
EOF

echo ""
echo "✨ 启动预览服务器..."
echo "📍 访问地址: http://localhost:5174"
echo ""
echo "💡 提示："
echo "   - 查看完整的液态玻璃效果"
echo "   - 测试响应式布局"
echo "   - 体验交互动画"
echo ""
echo "按 Ctrl+C 停止预览"
echo ""

# 启动开发服务器
npm run dev -- --config vite.config.preview.js

# 清理临时文件
echo ""
echo "🧹 清理临时文件..."
rm -f src/main-preview.js vite.config.preview.js

if [ -f "vite.config.js.backup" ]; then
    mv vite.config.js.backup vite.config.js
fi

echo "✅ 完成！"
