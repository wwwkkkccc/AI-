<template>
  <!-- 液态玻璃风格的登录/注册页面 -->
  <div class="glass-auth-page">
    <!-- 背景装饰 -->
    <div class="glass-bg-decoration">
      <div class="glass-orb glass-orb-1"></div>
      <div class="glass-orb glass-orb-2"></div>
      <div class="glass-orb glass-orb-3"></div>
    </div>

    <!-- 主容器 -->
    <div class="glass-auth-container">
      <!-- 左侧展示区 -->
      <section class="glass-auth-showcase">
        <div class="showcase-content">
          <span class="glass-eyebrow">Resume AI Workspace</span>
          <h1 class="showcase-title">
            让简历优化、岗位匹配与管理<br />
            协同落在同一张工作台上
          </h1>
          <p class="showcase-desc">
            参考国际大厂产品常见的信息分层与留白节奏，保留你现有功能，
            重做工作台骨架，让分析路径、结果反馈和管理入口都更清楚。
          </p>
        </div>

        <!-- 特性卡片 -->
        <div class="glass-feature-grid">
          <article
            v-for="item in authHighlights"
            :key="item.title"
            class="glass-feature-card"
          >
            <span class="feature-tag">{{ item.tag }}</span>
            <h3 class="feature-title">{{ item.title }}</h3>
            <p class="feature-desc">{{ item.desc }}</p>
          </article>
        </div>
      </section>

      <!-- 右侧表单区 -->
      <div class="glass-auth-form-wrapper">
        <form class="glass-auth-form" @submit.prevent="submitAuth">
          <!-- 表单头部 -->
          <div class="form-header">
            <span class="glass-badge">
              {{ authMode === 'login' ? '欢迎回来' : '创建账号' }}
            </span>
            <h2 class="form-title">
              {{ authMode === 'login' ? '登录工作台' : '注册新账号' }}
            </h2>
            <p class="form-subtitle">
              {{
                authMode === 'login'
                  ? '继续你的简历分析与管理任务。'
                  : '几秒内完成注册，开始体验完整分析闭环。'
              }}
            </p>
          </div>

          <!-- 用户名输入 -->
          <div class="glass-input-group">
            <label class="glass-label">用户名</label>
            <input
              v-model.trim="activeAuthForm.username"
              type="text"
              class="glass-input"
              placeholder="请输入用户名"
              required
            />
          </div>

          <!-- 密码输入 -->
          <div class="glass-input-group">
            <label class="glass-label">密码</label>
            <input
              v-model.trim="activeAuthForm.password"
              type="password"
              class="glass-input"
              placeholder="请输入密码"
              required
            />
          </div>

          <!-- 记住我 & 切换模式 -->
          <div class="form-options">
            <label class="glass-checkbox">
              <input
                id="remember"
                v-model="rememberMe"
                type="checkbox"
              />
              <span class="checkbox-label">记住我</span>
            </label>
            <a
              href="#"
              class="glass-link"
              @click.prevent="toggleAuthMode"
            >
              {{ authMode === 'login' ? '去注册' : '去登录' }}
            </a>
          </div>

          <!-- 提交按钮 -->
          <button
            type="submit"
            class="glass-btn glass-btn-primary glass-btn-block"
            :disabled="authLoading"
          >
            <span v-if="authLoading" class="btn-loading">
              <span class="loading-spinner"></span>
              提交中...
            </span>
            <span v-else>
              {{ authMode === 'login' ? '登录' : '注册' }}
            </span>
          </button>

          <!-- 底部提示 -->
          <div class="form-footer">
            <p v-if="authMode === 'login'" class="footer-text">
              还没有账号？
              <a
                href="#"
                class="glass-link"
                @click.prevent="setAuthMode('register')"
              >
                立即注册
              </a>
            </p>
            <p v-else class="footer-text">
              已有账号？
              <a
                href="#"
                class="glass-link"
                @click.prevent="setAuthMode('login')"
              >
                立即登录
              </a>
            </p>
          </div>

          <!-- 消息提示 -->
          <p v-if="authMessage" class="glass-message" :class="{ error: authMessage.includes('失败') }">
            {{ authMessage }}
          </p>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

// Props（从父组件传入）
const props = defineProps({
  authMode: {
    type: String,
    default: 'login'
  },
  authLoading: {
    type: Boolean,
    default: false
  },
  authMessage: {
    type: String,
    default: ''
  },
  rememberMe: {
    type: Boolean,
    default: false
  }
});

// Emits
const emit = defineEmits(['submit', 'toggleMode', 'setMode', 'update:rememberMe']);

// 表单数据
const activeAuthForm = ref({
  username: '',
  password: ''
});

// 特性展示数据
const authHighlights = [
  {
    tag: 'ATS',
    title: '智能评分',
    desc: '基于 ATS 规则的简历评分系统'
  },
  {
    tag: 'AI',
    title: 'GPT 优化',
    desc: '使用大语言模型优化简历内容'
  },
  {
    tag: 'KB',
    title: '题库管理',
    desc: '面试题库智能匹配与推荐'
  }
];

// 方法
const submitAuth = () => {
  emit('submit', activeAuthForm.value);
};

const toggleAuthMode = () => {
  emit('toggleMode');
};

const setAuthMode = (mode) => {
  emit('setMode', mode);
};
</script>

<style lang="scss" scoped>
@import '@/styles/liquid-glass.scss';

// ==================== 页面容器 ====================

.glass-auth-page {
  min-height: 100vh;
  background: linear-gradient(
    180deg,
    $dark-bg-start 0%,
    $dark-bg-end 100%
  );
  color: #ffffff;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

// ==================== 背景装饰 ====================

.glass-bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  overflow: hidden;
}

.glass-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
  animation: liquid-float 8s ease-in-out infinite;

  &.glass-orb-1 {
    width: 400px;
    height: 400px;
    background: radial-gradient(circle, rgba($ios-blue, 0.4) 0%, transparent 70%);
    top: -200px;
    left: -200px;
    animation-delay: 0s;
  }

  &.glass-orb-2 {
    width: 500px;
    height: 500px;
    background: radial-gradient(circle, rgba(138, 43, 226, 0.3) 0%, transparent 70%);
    bottom: -250px;
    right: -250px;
    animation-delay: 2s;
  }

  &.glass-orb-3 {
    width: 300px;
    height: 300px;
    background: radial-gradient(circle, rgba(255, 20, 147, 0.3) 0%, transparent 70%);
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    animation-delay: 4s;
  }
}

// ==================== 主容器 ====================

.glass-auth-container {
  @include liquid-glass-base(24px, $glass-bg-medium, $glass-border-light);

  max-width: 1200px;
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  border-radius: 24px;
  overflow: hidden;
  position: relative;
  z-index: 1;

  @media (max-width: 968px) {
    grid-template-columns: 1fr;
  }
}

// ==================== 左侧展示区 ====================

.glass-auth-showcase {
  padding: 64px 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;

  @media (max-width: 968px) {
    padding: 40px 32px;
  }

  .showcase-content {
    margin-bottom: 48px;
  }

  .glass-eyebrow {
    display: inline-block;
    font-size: 12px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 1px;
    color: rgba(255, 255, 255, 0.6);
    margin-bottom: 16px;
  }

  .showcase-title {
    font-size: 36px;
    font-weight: 700;
    line-height: 1.2;
    margin: 0 0 24px 0;
    background: linear-gradient(
      135deg,
      #ffffff 0%,
      rgba(255, 255, 255, 0.7) 100%
    );
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;

    @media (max-width: 968px) {
      font-size: 28px;
    }
  }

  .showcase-desc {
    font-size: 16px;
    line-height: 1.6;
    color: rgba(255, 255, 255, 0.7);
    margin: 0;
  }
}

// ==================== 特性卡片 ====================

.glass-feature-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

.glass-feature-card {
  @include liquid-glass-base(16px, $glass-bg-light, $glass-border-light);
  @include liquid-glass-hover;

  padding: 20px;
  border-radius: 16px;
  cursor: pointer;

  .feature-tag {
    display: inline-block;
    padding: 4px 8px;
    font-size: 11px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    border-radius: 6px;
    background: rgba($ios-blue, 0.2);
    color: rgba(255, 255, 255, 0.9);
    margin-bottom: 12px;
  }

  .feature-title {
    font-size: 18px;
    font-weight: 600;
    margin: 0 0 8px 0;
    color: #ffffff;
  }

  .feature-desc {
    font-size: 14px;
    line-height: 1.5;
    color: rgba(255, 255, 255, 0.7);
    margin: 0;
  }
}

// ==================== 右侧表单区 ====================

.glass-auth-form-wrapper {
  padding: 64px 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.05) 0%,
    rgba(255, 255, 255, 0.02) 100%
  );

  @media (max-width: 968px) {
    padding: 40px 32px;
  }
}

.glass-auth-form {
  width: 100%;
  max-width: 400px;

  .form-header {
    text-align: center;
    margin-bottom: 32px;

    .glass-badge {
      display: inline-block;
      padding: 6px 12px;
      font-size: 12px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      border-radius: 8px;
      background: linear-gradient(
        135deg,
        rgba($ios-blue, 0.3) 0%,
        rgba($ios-blue, 0.2) 100%
      );
      border: 1px solid rgba($ios-blue, 0.4);
      color: rgba(255, 255, 255, 0.9);
      margin-bottom: 16px;
    }

    .form-title {
      font-size: 28px;
      font-weight: 700;
      margin: 0 0 12px 0;
      color: #ffffff;
    }

    .form-subtitle {
      font-size: 14px;
      line-height: 1.5;
      color: rgba(255, 255, 255, 0.7);
      margin: 0;
    }
  }
}

// ==================== 输入框 ====================

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

// ==================== 表单选项 ====================

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .glass-checkbox {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;

    input[type="checkbox"] {
      width: 18px;
      height: 18px;
      cursor: pointer;
      accent-color: $ios-blue;
    }

    .checkbox-label {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.8);
    }
  }

  .glass-link {
    font-size: 14px;
    color: $ios-blue;
    text-decoration: none;
    transition: color 0.2s;

    &:hover {
      color: $ios-blue-hover;
    }
  }
}

// ==================== 按钮 ====================

.glass-btn {
  @include liquid-glass-button('primary');

  &.glass-btn-block {
    width: 100%;
  }

  .btn-loading {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;

    .loading-spinner {
      width: 16px;
      height: 16px;
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-top-color: #ffffff;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

// ==================== 表单底部 ====================

.form-footer {
  margin-top: 24px;
  text-align: center;

  .footer-text {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.7);
    margin: 0;

    .glass-link {
      color: $ios-blue;
      text-decoration: none;
      font-weight: 500;
      transition: color 0.2s;

      &:hover {
        color: $ios-blue-hover;
      }
    }
  }
}

// ==================== 消息提示 ====================

.glass-message {
  @include liquid-glass-base(12px, rgba($ios-blue, 0.2), rgba($ios-blue, 0.4));

  margin-top: 16px;
  padding: 12px 16px;
  font-size: 14px;
  text-align: center;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.9);

  &.error {
    background: linear-gradient(
      135deg,
      rgba(255, 59, 48, 0.2) 0%,
      rgba(255, 59, 48, 0.1) 100%
    );
    border-color: rgba(255, 59, 48, 0.4);
  }
}

// ==================== 响应式 ====================

@media (max-width: 968px) {
  .glass-auth-showcase {
    .showcase-content {
      margin-bottom: 32px;
    }

    .glass-feature-grid {
      display: none; // 移动端隐藏特性卡片
    }
  }
}
</style>
