# iOS 26 液态玻璃（Liquid Glass）样式系统使用指南

## 📦 项目结构

```
frontend/src/
├── styles/
│   └── liquid-glass.scss          # 核心样式库
├── components/
│   └── LiquidGlassDemo.vue        # 示例组件
└── App.vue                         # 主应用（待重构）
```

## 🚀 快速开始

### 1. 在组件中引入样式

```vue
<style lang="scss" scoped>
@import '@/styles/liquid-glass.scss';

.my-component {
  @include liquid-glass-card;
}
</style>
```

### 2. 使用预设类名（无需 SCSS）

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

## 🎨 核心 Mixin 使用

### 基础玻璃效果

```scss
.my-element {
  @include liquid-glass-base(
    $blur: 20px,                    // 模糊半径
    $bg: $glass-bg-medium,          // 背景色
    $border: $glass-border-light    // 边框色
  );
}
```

### 完整玻璃效果（含交互）

```scss
.my-card {
  @include liquid-glass-full;  // 包含 hover 和 active 效果
}
```

### 按钮样式

```scss
.my-button {
  @include liquid-glass-button('primary');    // 主按钮
  // 或
  @include liquid-glass-button('secondary');  // 次要按钮
}
```

### 卡片样式

```scss
.my-card {
  @include liquid-glass-card($padding: 24px);
}
```

### 输入框样式

```scss
.my-input {
  @include liquid-glass-input;
}
```

### 导航栏样式

```scss
.my-navbar {
  @include liquid-glass-navbar(false);  // 未滚动状态

  &.scrolled {
    @include liquid-glass-navbar(true); // 滚动后状态
  }
}
```

### 模态框样式

```scss
.my-modal {
  @include liquid-glass-modal;
}
```

## 🎯 实战示例

### 示例 1：重构登录表单

**原始代码：**
```vue
<template>
  <form class="auth-form">
    <input type="text" placeholder="用户名" />
    <button type="submit">登录</button>
  </form>
</template>

<style>
.auth-form {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}
</style>
```

**液态玻璃重构：**
```vue
<template>
  <form class="glass-auth-form">
    <input type="text" class="glass-input" placeholder="用户名" />
    <button type="submit" class="glass-btn glass-btn-primary">登录</button>
  </form>
</template>

<style lang="scss" scoped>
@import '@/styles/liquid-glass.scss';

.glass-auth-form {
  @include liquid-glass-card(32px);
  max-width: 400px;
  margin: 0 auto;

  .glass-input {
    @include liquid-glass-input;
    margin-bottom: 16px;
  }

  .glass-btn {
    @include liquid-glass-button('primary');
    width: 100%;
  }
}
</style>
```

### 示例 2：重构侧边栏导航

```vue
<template>
  <aside class="glass-sidebar">
    <div class="sidebar-header">
      <h1>Resume AI</h1>
    </div>

    <nav class="sidebar-nav">
      <button
        v-for="item in navItems"
        :key="item.key"
        class="nav-item"
        :class="{ active: activeTab === item.key }"
        @click="activeTab = item.key"
      >
        <span class="nav-badge">{{ item.badge }}</span>
        <span class="nav-label">{{ item.label }}</span>
      </button>
    </nav>
  </aside>
</template>

<style lang="scss" scoped>
@import '@/styles/liquid-glass.scss';

.glass-sidebar {
  @include liquid-glass-base(24px, $glass-bg-medium, $glass-border-light);

  width: 280px;
  height: 100vh;
  padding: 24px;
  position: fixed;
  left: 0;
  top: 0;

  .sidebar-header {
    margin-bottom: 32px;

    h1 {
      font-size: 24px;
      background: linear-gradient(135deg, #fff 0%, rgba(255, 255, 255, 0.7) 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
  }

  .sidebar-nav {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .nav-item {
    @include liquid-glass-base(16px, $glass-bg-light, $glass-border-light);
    @include liquid-glass-hover;

    padding: 12px 16px;
    display: flex;
    align-items: center;
    gap: 12px;
    border: none;
    cursor: pointer;
    text-align: left;
    color: rgba(255, 255, 255, 0.8);

    &.active {
      background: linear-gradient(
        135deg,
        rgba($ios-blue, 0.3) 0%,
        rgba($ios-blue, 0.2) 100%
      );
      border-color: rgba($ios-blue, 0.4);
      color: #ffffff;
    }

    .nav-badge {
      font-size: 12px;
      font-weight: 600;
      opacity: 0.7;
    }

    .nav-label {
      font-size: 15px;
      font-weight: 500;
    }
  }
}
</style>
```

### 示例 3：重构数据卡片网格

```vue
<template>
  <div class="glass-stats-grid">
    <article
      v-for="stat in stats"
      :key="stat.label"
      class="glass-stat-card"
    >
      <span class="stat-label">{{ stat.label }}</span>
      <strong class="stat-value">{{ stat.value }}</strong>
      <p class="stat-desc">{{ stat.desc }}</p>
    </article>
  </div>
</template>

<style lang="scss" scoped>
@import '@/styles/liquid-glass.scss';

.glass-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.glass-stat-card {
  @include liquid-glass-card(20px);
  @include liquid-glass-hover;

  text-align: center;
  cursor: pointer;

  .stat-label {
    display: block;
    font-size: 12px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: rgba(255, 255, 255, 0.6);
    margin-bottom: 8px;
  }

  .stat-value {
    display: block;
    font-size: 32px;
    font-weight: 700;
    color: #ffffff;
    margin-bottom: 4px;
  }

  .stat-desc {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.7);
    margin: 0;
  }
}
</style>
```

## 🎭 进阶效果

### 1. 边缘发光

```scss
.my-element {
  @include liquid-glass-full;
  @include liquid-glass-glow($ios-blue, 0.3);
}
```

### 2. 组件融合

```scss
.my-list-item {
  @include liquid-glass-full;
  @include liquid-glass-merge;  // 相邻元素边缘融合
}
```

### 3. 响应式适配

```scss
.my-component {
  @include liquid-glass-card;
  @include liquid-glass-responsive;  // 移动端优化
}
```

### 4. 自定义动画

```scss
.my-floating-card {
  @include liquid-glass-card;

  animation: liquid-float 3s ease-in-out infinite;
}

.my-pulsing-button {
  @include liquid-glass-button('primary');

  animation: liquid-pulse 2s ease-in-out infinite;
}
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

// 阴影
$glass-shadow-light: rgba(0, 0, 0, 0.05);
$glass-shadow-medium: rgba(0, 0, 0, 0.1);
$glass-shadow-heavy: rgba(0, 0, 0, 0.15);

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
  // 移动端样式
  .my-component {
    border-radius: 12px;
    padding: 16px;
    backdrop-filter: blur(16px);
  }
}
```

## ⚡ 性能优化建议

1. **减少模糊半径**：移动端使用 `blur(16px)` 而非 `blur(24px)`
2. **避免过度嵌套**：不要在玻璃容器内嵌套过多玻璃子元素
3. **使用 will-change**：对频繁动画的元素添加 `will-change: transform`
4. **懒加载背景**：大图背景使用懒加载

```scss
.optimized-glass {
  @include liquid-glass-base(16px);  // 降低模糊半径
  will-change: transform;             // 优化动画性能
}
```

## 🔧 浏览器兼容性

- **Chrome/Edge**: 完全支持
- **Safari**: 完全支持（需要 `-webkit-` 前缀）
- **Firefox**: 部分支持（`backdrop-filter` 需开启实验性功能）

**兼容性处理：**
```scss
.my-glass {
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);  // Safari 支持

  // Firefox 降级方案
  @supports not (backdrop-filter: blur(20px)) {
    background: rgba(255, 255, 255, 0.3);
  }
}
```

## 📚 完整重构步骤

### 步骤 1：全局样式设置

在 `App.vue` 的 `<style>` 中添加：

```scss
@import '@/styles/liquid-glass.scss';

body {
  background: linear-gradient(180deg, $dark-bg-start 0%, $dark-bg-end 100%);
  color: #ffffff;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}
```

### 步骤 2：重构核心组件

1. **登录/注册表单** → 使用 `liquid-glass-card` + `liquid-glass-input`
2. **侧边栏导航** → 使用 `liquid-glass-base` + 自定义导航项
3. **主内容卡片** → 使用 `liquid-glass-card`
4. **按钮** → 使用 `liquid-glass-button`
5. **模态框** → 使用 `liquid-glass-modal`

### 步骤 3：添加交互效果

```vue
<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const isScrolled = ref(false);

const handleScroll = () => {
  isScrolled.value = window.scrollY > 50;
};

onMounted(() => {
  window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>
```

### 步骤 4：测试与优化

1. 在不同设备上测试响应式效果
2. 检查性能（Chrome DevTools Performance）
3. 调整模糊半径和透明度
4. 优化动画流畅度

## 🎉 完成！

现在您的 Vue 项目已经拥有了完整的 iOS 26 液态玻璃风格！

如需更多帮助，请参考：
- `LiquidGlassDemo.vue` - 完整示例组件
- `liquid-glass.scss` - 核心样式库
