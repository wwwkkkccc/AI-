<template>
  <div class="liquid-glass-demo">
    <!-- 液态玻璃导航栏 -->
    <nav class="glass-navbar" :class="{ scrolled: isScrolled }">
      <div class="navbar-content">
        <h1 class="navbar-brand">Resume AI</h1>
        <div class="navbar-actions">
          <button class="glass-btn glass-btn-secondary">设置</button>
          <button class="glass-btn glass-btn-primary">登录</button>
        </div>
      </div>
    </nav>

    <!-- 主内容区 -->
    <main class="glass-main">
      <!-- 欢迎卡片 -->
      <section class="glass-card glass-card-hero">
        <span class="glass-badge">AI Workspace</span>
        <h2>让简历优化更智能</h2>
        <p>基于 AI 的简历分析与优化平台，帮助您打造完美简历</p>
        <div class="glass-card-actions">
          <button class="glass-btn glass-btn-primary glass-btn-large">
            开始分析
          </button>
          <button class="glass-btn glass-btn-secondary glass-btn-large">
            了解更多
          </button>
        </div>
      </section>

      <!-- 功能卡片网格 -->
      <div class="glass-grid">
        <article
          v-for="feature in features"
          :key="feature.title"
          class="glass-card glass-card-feature"
        >
          <div class="feature-icon">{{ feature.icon }}</div>
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.description }}</p>
        </article>
      </div>

      <!-- 输入表单示例 -->
      <section class="glass-card glass-card-form">
        <h3>快速体验</h3>
        <form @submit.prevent="handleSubmit">
          <div class="glass-input-group">
            <label class="glass-label">用户名</label>
            <input
              v-model="form.username"
              type="text"
              class="glass-input"
              placeholder="请输入用户名"
            />
          </div>

          <div class="glass-input-group">
            <label class="glass-label">密码</label>
            <input
              v-model="form.password"
              type="password"
              class="glass-input"
              placeholder="请输入密码"
            />
          </div>

          <button type="submit" class="glass-btn glass-btn-primary glass-btn-block">
            提交
          </button>
        </form>
      </section>

      <!-- 模态框示例 -->
      <div v-if="showModal" class="glass-modal-overlay" @click="showModal = false">
        <div class="glass-modal" @click.stop>
          <button class="glass-modal-close" @click="showModal = false">×</button>
          <h3>操作成功</h3>
          <p>您的简历已成功提交分析</p>
          <div class="glass-modal-actions">
            <button class="glass-btn glass-btn-secondary" @click="showModal = false">
              取消
            </button>
            <button class="glass-btn glass-btn-primary" @click="showModal = false">
              确认
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

// 响应式数据
const isScrolled = ref(false);
const showModal = ref(false);
const form = ref({
  username: '',
  password: ''
});

const features = [
  {
    icon: '📊',
    title: 'ATS 评分',
    description: '智能分析简历与岗位匹配度'
  },
  {
    icon: '✨',
    title: 'AI 优化',
    description: '基于 GPT 的简历优化建议'
  },
  {
    icon: '🎯',
    title: '岗位推荐',
    description: '精准匹配适合的工作机会'
  },
  {
    icon: '📝',
    title: '模板生成',
    description: '一键生成专业简历模板'
  }
];

// 滚动监听
const handleScroll = () => {
  isScrolled.value = window.scrollY > 50;
};

// 表单提交
const handleSubmit = () => {
  console.log('Form submitted:', form.value);
  showModal.value = true;
};

// 生命周期
onMounted(() => {
  window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<style lang="scss" scoped>
@import '@/styles/liquid-glass.scss';

// ==================== 全局容器 ====================

.liquid-glass-demo {
  min-height: 100vh;
  background: linear-gradient(
    180deg,
    $dark-bg-start 0%,
    $dark-bg-end 100%
  );
  color: #ffffff;
  padding-top: 80px;
}

// ==================== 导航栏 ====================

.glass-navbar {
  @include liquid-glass-navbar(false);

  &.scrolled {
    @include liquid-glass-navbar(true);
  }

  .navbar-content {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .navbar-brand {
    font-size: 20px;
    font-weight: 600;
    margin: 0;
    background: linear-gradient(135deg, #ffffff 0%, rgba(255, 255, 255, 0.7) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .navbar-actions {
    display: flex;
    gap: 12px;
  }
}

// ==================== 主内容区 ====================

.glass-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px;
}

// ==================== 卡片样式 ====================

.glass-card {
  @include liquid-glass-card(32px);
  @include liquid-glass-responsive;

  margin-bottom: 24px;

  h2, h3 {
    margin: 0 0 12px 0;
    font-weight: 600;
  }

  p {
    margin: 0 0 16px 0;
    color: rgba(255, 255, 255, 0.8);
    line-height: 1.6;
  }
}

// 英雄卡片
.glass-card-hero {
  text-align: center;
  padding: 64px 32px;

  h2 {
    font-size: 48px;
    margin-bottom: 16px;
    background: linear-gradient(135deg, #ffffff 0%, rgba(255, 255, 255, 0.8) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  p {
    font-size: 18px;
    margin-bottom: 32px;
  }

  .glass-card-actions {
    display: flex;
    gap: 16px;
    justify-content: center;
  }
}

// 功能卡片
.glass-card-feature {
  text-align: center;
  padding: 32px 24px;
  cursor: pointer;

  @include liquid-glass-hover;

  .feature-icon {
    font-size: 48px;
    margin-bottom: 16px;
    animation: liquid-float 3s ease-in-out infinite;
  }

  h3 {
    font-size: 20px;
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    margin: 0;
  }
}

// 表单卡片
.glass-card-form {
  max-width: 500px;
  margin: 0 auto 24px;

  h3 {
    font-size: 24px;
    margin-bottom: 24px;
    text-align: center;
  }
}

// ==================== 网格布局 ====================

.glass-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 24px;
  margin-bottom: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

// ==================== 按钮样式 ====================

.glass-btn {
  @include liquid-glass-button('primary');

  &.glass-btn-secondary {
    @include liquid-glass-button('secondary');
  }

  &.glass-btn-large {
    padding: 16px 32px;
    font-size: 16px;
  }

  &.glass-btn-block {
    width: 100%;
  }
}

// ==================== 输入框样式 ====================

.glass-input-group {
  margin-bottom: 20px;

  .glass-label {
    display: block;
    margin-bottom: 8px;
    font-size: 14px;
    font-weight: 500;
    color: rgba(255, 255, 255, 0.9);
  }

  .glass-input {
    @include liquid-glass-input;
  }
}

// ==================== 徽章样式 ====================

.glass-badge {
  display: inline-block;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-radius: 8px;
  margin-bottom: 16px;

  background: linear-gradient(
    135deg,
    rgba($ios-blue, 0.3) 0%,
    rgba($ios-blue, 0.2) 100%
  );
  border: 1px solid rgba($ios-blue, 0.4);
  color: rgba(255, 255, 255, 0.9);
}

// ==================== 模态框样式 ====================

.glass-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 24px;
}

.glass-modal {
  @include liquid-glass-modal;

  position: relative;
  animation: liquid-float 0.3s ease-out;

  h3 {
    font-size: 24px;
    margin-bottom: 16px;
  }

  p {
    margin-bottom: 24px;
    color: rgba(255, 255, 255, 0.8);
  }

  .glass-modal-close {
    position: absolute;
    top: 16px;
    right: 16px;
    width: 32px;
    height: 32px;
    border: none;
    background: rgba(255, 255, 255, 0.1);
    color: #ffffff;
    font-size: 24px;
    line-height: 1;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: rgba(255, 255, 255, 0.2);
    }
  }

  .glass-modal-actions {
    display: flex;
    gap: 12px;
    justify-content: flex-end;
  }
}

// ==================== 响应式适配 ====================

@media (max-width: 768px) {
  .glass-navbar {
    .navbar-content {
      padding: 0 16px;
    }

    .navbar-brand {
      font-size: 18px;
    }
  }

  .glass-card-hero {
    padding: 48px 24px;

    h2 {
      font-size: 32px;
    }

    p {
      font-size: 16px;
    }

    .glass-card-actions {
      flex-direction: column;

      .glass-btn {
        width: 100%;
      }
    }
  }

  .glass-modal {
    padding: 24px;
    max-width: 100%;
  }
}
</style>
