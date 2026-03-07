<template>
  <div class="sub-card">
    <h3>隐私脱敏</h3>

    <div class="redaction-panel">
      <!-- 输入区 -->
      <div class="input-section">
        <label>原始文本</label>
        <textarea
          v-model.trim="redactionState.inputText"
          rows="10"
          placeholder="粘贴需要脱敏的简历或文本内容"
        />
        <button :disabled="loading || !redactionState.inputText" @click="$emit('redact')">
          {{ loading ? '脱敏中...' : '开始脱敏' }}
        </button>
      </div>

      <!-- 脱敏结果 -->
      <div v-if="redactionState.outputText" class="output-section">
        <div class="output-header">
          <label>脱敏后文本</label>
          <button class="mini-btn" @click="$emit('copy')">
            {{ redactionState.copied ? '已复制' : '复制文本' }}
          </button>
        </div>
        <div class="output-text" v-html="highlightRedacted(redactionState.outputText)"></div>

        <!-- 脱敏统计 -->
        <div v-if="redactionState.stats" class="stats-section">
          <h4>脱敏统计</h4>
          <div class="stats-grid">
            <div v-if="redactionState.stats.phoneCount" class="stat-item">
              <span class="stat-label">手机号</span>
              <span class="stat-value">{{ redactionState.stats.phoneCount }}</span>
            </div>
            <div v-if="redactionState.stats.emailCount" class="stat-item">
              <span class="stat-label">邮箱</span>
              <span class="stat-value">{{ redactionState.stats.emailCount }}</span>
            </div>
            <div v-if="redactionState.stats.idCardCount" class="stat-item">
              <span class="stat-label">身份证</span>
              <span class="stat-value">{{ redactionState.stats.idCardCount }}</span>
            </div>
            <div v-if="redactionState.stats.addressCount" class="stat-item">
              <span class="stat-label">地址</span>
              <span class="stat-value">{{ redactionState.stats.addressCount }}</span>
            </div>
            <div v-if="redactionState.stats.nameCount" class="stat-item">
              <span class="stat-label">姓名</span>
              <span class="stat-value">{{ redactionState.stats.nameCount }}</span>
            </div>
          </div>
        </div>

        <!-- 对比视图 -->
        <div class="compare-section">
          <h4>脱敏对比</h4>
          <div class="compare-grid">
            <div class="compare-col">
              <strong>原始文本</strong>
              <pre>{{ redactionState.inputText }}</pre>
            </div>
            <div class="compare-col">
              <strong>脱敏文本</strong>
              <pre>{{ redactionState.outputText }}</pre>
            </div>
          </div>
        </div>
      </div>
    </div>

    <p class="message">{{ message }}</p>
  </div>
</template>

<script setup>
defineProps({
  redactionState: { type: Object, required: true },
  loading: { type: Boolean, default: false },
  message: { type: String, default: "" }
});

defineEmits(["redact", "copy"]);

function highlightRedacted(text) {
  if (!text) return "";
  // 高亮被替换的占位符
  return text
    .replace(/\[手机号\]/g, '<span class="redacted">[手机号]</span>')
    .replace(/\[邮箱\]/g, '<span class="redacted">[邮箱]</span>')
    .replace(/\[身份证\]/g, '<span class="redacted">[身份证]</span>')
    .replace(/\[地址\]/g, '<span class="redacted">[地址]</span>')
    .replace(/\[姓名\]/g, '<span class="redacted">[姓名]</span>')
    .replace(/\n/g, "<br>");
}
</script>

<style scoped>
.redaction-panel {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.input-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.output-section {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.output-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.output-text {
  padding: 1rem;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.output-text :deep(.redacted) {
  background: #fef3c7;
  color: #92400e;
  padding: 0.125rem 0.25rem;
  border-radius: 0.25rem;
  font-weight: 600;
}

.stats-section h4 {
  margin-bottom: 1rem;
  color: #374151;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 1rem;
}

.stat-item {
  padding: 1rem;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.stat-value {
  display: block;
  font-size: 1.5rem;
  font-weight: 700;
  color: #3b82f6;
}

.compare-section h4 {
  margin-bottom: 1rem;
  color: #374151;
}

.compare-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.compare-col {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.compare-col strong {
  color: #374151;
  font-size: 0.875rem;
}

.compare-col pre {
  padding: 1rem;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  white-space: pre-wrap;
  word-wrap: break-word;
  line-height: 1.6;
  margin: 0;
  font-size: 0.875rem;
}
</style>
