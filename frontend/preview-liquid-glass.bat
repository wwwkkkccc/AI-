@echo off
REM 液态玻璃样式系统 - 快速预览脚本 (Windows)

echo ========================================
echo 🎨 iOS 26 液态玻璃风格系统
echo ========================================
echo.

REM 检查是否在正确的目录
if not exist "frontend" (
    echo ❌ 错误：请在项目根目录运行此脚本
    pause
    exit /b 1
)

cd frontend

REM 检查 npm
where npm >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 错误：未安装 npm
    pause
    exit /b 1
)

echo 📦 检查依赖...
call npm list sass >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo 📥 安装 sass...
    call npm install -D sass
)

echo.
echo ✨ 启动预览服务器...
echo 📍 访问地址: http://localhost:5173
echo.
echo 💡 提示：
echo    - 查看完整的液态玻璃效果
echo    - 测试响应式布局
echo    - 体验交互动画
echo.
echo 按 Ctrl+C 停止预览
echo.

REM 启动开发服务器
call npm run dev

echo.
echo ✅ 完成！
pause
