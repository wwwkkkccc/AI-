# 🎨 iOS 26 液态玻璃风格系统

> 为 Resume AI 项目打造的现代化 UI 重构方案

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Vue](https://img.shields.io/badge/Vue-3.x-green.svg)
![SCSS](https://img.shields.io/badge/SCSS-1.x-pink.svg)

## 📦 包含内容

### 核心文件
- **`src/styles/liquid-glass.scss`** - 完整的 SCSS Mixin 库（400+ 行）
- **`src/components/LiquidGlassDemo.vue`** - 功能演示组件（500+ 行）
- **`src/components/GlassAuthForm.vue`** - 登录/注册表单（600+ 行）

### 文档
- **`LIQUID_GLASS_GUIDE.md`** - 详细使用指南（800+ 行）
- **`INTEGRATION_GUIDE.md`** - 集成步骤说明（300+ 行）
- **`LIQUID_GLASS_SUMMARY.md`** - 完整总结文档（350+ 行）

### 工具
- **`preview-liquid-glass.sh`** - Linux/Mac 预览脚本
- **`preview-liquid-glass.bat`** - Windows 预览脚本

## ✨ 核心特性

### 视觉效果
- ✅ 毛玻璃背景模糊（`backdrop-filter: blur(20px)`）
- ✅ 多层阴影叠加（外层投影 + 内层高光）
- ✅ 边缘高光效果（0.5px 白色边框）
- ✅ 渐变背景（半透明 rgba）
- ✅ 背景噪点纹理（SVG 噪点）

### 交互效果
- ✅ 悬停上浮（`transform: translateY(-2px)`）
- ✅ 点击按压（内阴影加深）
- ✅ 滚动透明度变化（导航栏）
- ✅ 流畅过渡动画（`cubic-bezier(0.4, 0, 0.2, 1)`）
- ✅ 加载状态动画（旋转 spinner）

### 进阶效果
- ✅ 背景动态扭曲（浮动光球动画）
- ✅ 多重阴影叠加（3层阴影）
- ✅ 边缘发光（`drop-shadow`）
- ✅ 组件融合（相邻元素边缘融合）

## 🚀 快速开始

### 1. 查看示例

**Windows:**
```bash
cd frontend
preview-liquid-glass.bat
```

**Linux/Mac:**
```bash
cd frontend
./preview-liquid-glass.sh
```

### 2. 在组件中使用

```vue
<template>
  <div class="liquid-glass-card">
    <h2>我的卡片</h2>
    <button class="liquid-glass-button">点击我</button>
  </div>
</template>

<style>
@import '@/styles/liquid-glass.scss';
</style>
```

### 3. 使用 Mixin 自定义

```vue
<style lang="scss" scoped>
@import '@/styles/liquid-glass.scss';

.my-component {
  @include liquid-glass-card(32px);
  @include liquid-glass-hover;
}
</style>
```

## 📚 文档导航

### 新手入门
1. 阅读 [LIQUID_GLASS_SUMMARY.md](./LIQUID_GLASS_SUMMARY.md) 了解全貌
2. 查看 [LiquidGlassDemo.vue](./src/components/LiquidGlassDemo.vue) 学习示例
3. 参考 [LIQUID_GLASS_GUIDE.md](./LIQUID_GLASS_GUIDE.md) 深入学习

### 开始集成
1. 阅读 [INTEGRATION_GUIDE.md](./INTEGRATION_GUIDE.md) 了解集成步骤
2. 使用 [GlassAuthForm.vue](./src/components/GlassAuthForm.vue) 替换现有表单
3. 逐步重构其他组件

## 🎯 核心 Mixin

### 基础效果
```scss
@include liquid-glass-base($blur, $bg, $border);
@include liquid-glass-hover();
@include liquid-glass-active();
@include liquid-glass-full();  // 包含 base + hover + active
```

### 组件样式
```scss
@include liquid-glass-button('primary');    // 主按钮
@include liquid-glass-button('secondary');  // 次要按钮
@include liquid-glass-card($padding);       // 卡片
@include liquid-glass-input();              // 输入框
@include liquid-glass-navbar($scrolled);    // 导航栏
@include liquid-glass-modal();              // 模态框
```

### 工具类
```scss
@include liquid-glass-glow($color, $intensity);  // 边缘发光
@include liquid-glass-merge();                   // 组件融合
@include liquid-glass-responsive();              // 响应式适配
```

## 🎨 颜色变量

```scss
// 玻璃背景
$glass-bg-light: rgba(255, 255, 255, 0.1);
$glass-bg-medium: rgba(255, 255, 255, 0.15);
$glass-bg-heavy: rgba(255, 255, 255, 0.2);

// 玻璃边框
$glass-border-light: rgba(255, 255, 255, 0.2);
$glass-border-medium: rgba(255, 255, 255, 0.3);
$glass-border-heavy: rgba(255, 255, 255, 0.4);

// iOS 蓝色
$ios-blue: #0A84FF;
$ios-blue-hover: #0A7AEF;
$ios-blue-active: #0070DD;

// 深色背景
$dark-bg-start: #000000;
$dark-bg-end: #1a1a1a;
```

## 📱 响应式断点

```scss
@media (max-width: 768px) {
  // 移动端优化
  backdrop-filter: blur(12px);  // 降低模糊半径
  padding: 16px;                // 减少内边距
}
```

## ⚡ 性能优化

### 1. 降低模糊半径
```scss
// 移动端使用较低的模糊半径
@media (max-width: 768px) {
  backdrop-filter: blur(12px);  // 而非 20px
}
```

### 2. 避免过度嵌套
```vue
<!-- ❌ 避免 -->
<div class="liquid-glass-card">
  <div class="liquid-glass-card">
    <div class="liquid-glass-card"></div>
  </div>
</div>

<!-- ✅ 推荐 -->
<div class="liquid-glass-card">
  <div class="card-content"></div>
</div>
```

### 3. 使用 will-change
```scss
.animated-element {
  will-change: transform;
  @include liquid-glass-full;
}
```

## 🐛 常见问题

### Q: 样式不生效？
**A:** 确保已安装 `sass` 依赖：
```bash
npm install -D sass
```

### Q: Firefox 不显示模糊效果？
**A:** Firefox 需要手动开启 `backdrop-filter`，或使用降级方案：
```scss
@supports not (backdrop-filter: blur(20px)) {
  background: rgba(255, 255, 255, 0.3);
}
```

### Q: 移动端性能差？
**A:** 降低模糊半径和减少阴影层数：
```scss
@media (max-width: 768px) {
  backdrop-filter: blur(12px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}
```

## 🌐 浏览器兼容性

| 浏览器 | 支持情况 | 备注 |
|--------|---------|------|
| Chrome | ✅ 完全支持 | 推荐使用 |
| Safari | ✅ 完全支持 | 需要 `-webkit-` 前缀 |
| Edge | ✅ 完全支持 | Chromium 内核 |
| Firefox | ⚠️ 部分支持 | 需开启实验性功能 |

## 📊 项目统计

- **总文件数**: 6 个
- **总代码行数**: 2600+ 行
- **核心样式**: 400+ 行 SCSS
- **示例组件**: 1100+ 行 Vue
- **文档说明**: 1100+ 行 Markdown

## 🎉 效果预览

### 登录表单
- 毛玻璃背景
- 浮动光球动画
- 流畅的输入交互
- 加载状态动画

### 卡片组件
- 悬停上浮效果
- 多层阴影叠加
- 边缘高光
- 响应式布局

### 按钮组件
- 点击按压反馈
- 渐变背景
- 禁用状态
- 加载动画

## 📝 更新日志

### v1.0.0 (2026-03-07)
- ✨ 初始发布
- ✅ 完整的 SCSS Mixin 库
- ✅ 2 个示例组件
- ✅ 3 份详细文档
- ✅ 预览脚本

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📮 联系方式

如有问题，请提交 Issue 或查看文档。

---

**Made with ❤️ for Resume AI**
