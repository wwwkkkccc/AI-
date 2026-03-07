<template>
  <div class="sub-card">
    <h3>AI 模拟面试</h3>
    <div v-if="!interviewState.sessionId" class="interview-start">
      <form @submit.prevent="$emit('start')">
        <label>目标岗位</label>
        <input v-model.trim="interviewState.form.targetRole" type="text" placeholder="例如：Java 高级后端工程师" required />
        <label>简历文本（可选，留空则使用当前分析结果）</label>
        <textarea v-model.trim="interviewState.form.resumeText" rows="4" placeholder="粘贴简历文本，或留空使用已有分析" />
        <label>岗位描述 JD（可选）</label>
        <textarea v-model.trim="interviewState.form.jdText" rows="4" placeholder="粘贴 JD 文本" />
        <button :disabled="loading">{{ loading ? '准备中...' : '开始模拟面试' }}</button>
      </form>
    </div>

    <div v-else class="interview-panel">
      <div class="interview-header">
        <div class="interview-meta">
          <span class="pill">{{ interviewState.targetRole }}</span>
          <span class="pill">第 {{ interviewState.questionCount }} 题</span>
          <span v-if="interviewState.totalScore != null" class="pill">
            平均得分：{{ formatScore(interviewState.totalScore) }}
          </span>
          <span class="state-pill" :class="interviewState.status === 'ACTIVE' ? 'state-yes' : 'state-no'">
            {{ interviewState.status === 'ACTIVE' ? '进行中' : '已结束' }}
          </span>
        </div>
        <button
          v-if="interviewState.status === 'ACTIVE'"
          class="mini-btn warn"
          :disabled="loading"
          @click="$emit('end')"
        >
          {{ loading ? '结束中...' : '结束面试' }}
        </button>
      </div>

      <div class="chat-list">
        <div
          v-for="msg in interviewState.messages || []"
          :key="`iv-${msg.id}`"
          class="chat-item"
          :class="msg.role === 'USER' ? 'chat-user' : 'chat-ai'"
        >
          <strong>{{ msg.role === 'USER' ? '我' : '面试官' }}</strong>
          <p>{{ msg.content }}</p>
          <div v-if="msg.score != null" class="interview-feedback">
            <span class="score-pill" :class="scoreLevelClass(msg.score)">{{ msg.score }} 分</span>
            <span v-if="msg.feedback" class="feedback-text">{{ msg.feedback }}</span>
          </div>
        </div>
      </div>

      <form v-if="interviewState.status === 'ACTIVE'" class="chat-form" @submit.prevent="$emit('answer')">
        <input
          v-model.trim="interviewState.input"
          type="text"
          placeholder="输入你的回答..."
        />
        <button :disabled="loading || !interviewState.input">
          {{ loading ? '提交中...' : '提交回答' }}
        </button>
      </form>

      <div v-if="interviewState.report" class="interview-report">
        <h4>面试总评报告</h4>
        <p class="report-summary">{{ interviewState.report.report }}</p>
        <div class="report-grid">
          <div v-if="interviewState.report.strengths?.length" class="report-section">
            <strong>强项</strong>
            <ul><li v-for="(s, i) in interviewState.report.strengths" :key="`s-${i}`">{{ s }}</li></ul>
          </div>
          <div v-if="interviewState.report.weaknesses?.length" class="report-section">
            <strong>待改进</strong>
            <ul><li v-for="(w, i) in interviewState.report.weaknesses" :key="`w-${i}`">{{ w }}</li></ul>
          </div>
          <div v-if="interviewState.report.suggestions?.length" class="report-section">
            <strong>建议</strong>
            <ul><li v-for="(g, i) in interviewState.report.suggestions" :key="`g-${i}`">{{ g }}</li></ul>
          </div>
        </div>
      </div>
    </div>
    <p class="message">{{ message }}</p>
  </div>
</template>

<script setup>
defineProps({
  interviewState: { type: Object, required: true },
  loading: { type: Boolean, default: false },
  message: { type: String, default: "" }
});

defineEmits(["start", "answer", "end"]);

function formatScore(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n.toFixed(1) : "0";
}

function scoreLevelClass(score) {
  const n = Number(score) || 0;
  if (n >= 80) return "score-high";
  if (n >= 60) return "score-mid";
  return "score-low";
}
</script>
