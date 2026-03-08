<template>
  <section class="stack">
    <article class="panel-card">
      <h3>1) 提交分析任务</h3>
      <form class="form-grid" @submit.prevent="submitAnalyze">
        <label class="field">
          <span>目标岗位</span>
          <input v-model.trim="analyzeForm.targetRole" type="text" placeholder="例如：高级后端工程师" />
        </label>
        <label class="field">
          <span>简历文件</span>
          <input
            type="file"
            accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg,.bmp,.webp,.tif,.tiff"
            @change="onFileChange"
            required
          />
        </label>
        <label class="field">
          <span>JD 文本</span>
          <textarea v-model.trim="analyzeForm.jdText" rows="5" />
        </label>
        <label class="field">
          <span>JD 图片（可选）</span>
          <input type="file" accept=".png,.jpg,.jpeg,.bmp,.webp,.tif,.tiff" @change="onJdImageChange" />
        </label>

        <div class="actions">
          <button class="btn primary" :disabled="analyzeLoading" type="submit">
            {{ analyzeLoading ? "提交中..." : "提交分析" }}
          </button>
          <button class="btn ghost" type="button" :disabled="!queueJob.jobId" @click="refreshCurrentJob">刷新任务</button>
        </div>
      </form>

      <p class="message">{{ analyzeMessage }}</p>
      <div v-if="queueJob.jobId" class="queue-grid">
        <div><span>任务 ID</span><strong>{{ queueJob.jobId }}</strong></div>
        <div><span>状态</span><strong :class="statusClass(queueJob.status)">{{ statusText(queueJob.status) }}</strong></div>
        <div><span>队列位置</span><strong>{{ queueJob.queuePosition ?? "-" }}</strong></div>
        <div><span>优先级</span><strong>{{ queueJob.vipPriority ? "VIP" : "标准" }}</strong></div>
      </div>
    </article>

    <article v-if="result" class="panel-card">
      <h3>2) 分析结果</h3>
      <div class="metric-grid">
        <div class="metric"><span>评分</span><strong>{{ formatScore(result.score) }}</strong></div>
        <div class="metric"><span>匹配度</span><strong>{{ formatCoverage(result.coverage) }}</strong></div>
      </div>

      <p><strong>匹配关键词：</strong> {{ (result.matchedKeywords || []).join(", ") || "-" }}</p>
      <p><strong>缺失关键词：</strong> {{ (result.missingKeywords || []).join(", ") || "-" }}</p>
      <p><strong>总结：</strong> {{ result.optimized?.summary || "-" }}</p>

      <div class="actions">
        <button class="btn ghost" :disabled="!result.optimizedResumeMarkdown" @click="copyText(result.optimizedResumeMarkdown)">
          复制优化简历
        </button>
        <button
          class="btn ghost"
          :disabled="!result.optimizedResumeMarkdown"
          @click="downloadText('optimized-resume.md', result.optimizedResumeMarkdown)"
        >
          下载 .md
        </button>
      </div>

      <pre v-if="result.optimizedResumeMarkdown" class="code-preview">{{ result.optimizedResumeMarkdown }}</pre>
    </article>

    <article class="panel-card">
      <h3>3) 生成 / 改写</h3>
      <form class="form-grid" @submit.prevent="generateResumeFromJd">
        <label class="field"><span>目标岗位</span><input v-model.trim="resumeGenForm.targetRole" type="text" required /></label>
        <label class="field"><span>JD 文本</span><textarea v-model.trim="resumeGenForm.jdText" rows="4" required /></label>
        <label class="field"><span>背景信息（可选）</span><textarea v-model.trim="resumeGenForm.userBackground" rows="3" /></label>

        <div class="actions">
          <button class="btn primary" :disabled="resumeGenLoading" type="submit">
            {{ resumeGenLoading ? "生成中..." : "从 JD 生成" }}
          </button>
          <button class="btn ghost" type="button" :disabled="resumeGenLoading || !result?.analysisId" @click="rewriteResumeFromCurrentAnalysis">
            基于当前分析改写
          </button>
        </div>
      </form>

      <p class="message">{{ resumeGenMessage }}</p>
      <div v-if="generatedResume.markdown">
        <div class="actions">
          <button class="btn ghost" @click="copyGeneratedResume">复制</button>
          <button class="btn ghost" @click="downloadGeneratedResume">下载</button>
        </div>
        <pre class="code-preview">{{ generatedResume.markdown }}</pre>
      </div>
    </article>

    <article class="panel-card">
      <h3>4) 聊天优化</h3>
      <div class="actions">
        <button class="btn ghost" :disabled="chatLoading || !result?.analysisId" @click="startResumeChat">开始会话</button>
        <button class="btn ghost" :disabled="chatLoading || !chatState.sessionId" @click="quickAsk('请给我 3 条最高优先级改进建议')">
          快速提问
        </button>
      </div>
      <p class="message">{{ chatMessage }}</p>

      <div class="chat-box">
        <div v-for="msg in chatState.messages" :key="msg.id || `${msg.role}-${msg.createdAt}`" class="chat-item">
          <strong>{{ chatRoleText(msg.role) }}</strong>
          <p>{{ msg.content }}</p>
        </div>
      </div>

      <form class="inline-form" @submit.prevent="sendChatMessage">
        <input v-model.trim="chatState.input" type="text" placeholder="输入你的问题..." />
        <button class="btn primary" :disabled="chatLoading || !chatState.input" type="submit">发送</button>
      </form>
    </article>

    <div class="split-row">
      <article class="panel-card">
        <h3>5) JD 雷达</h3>
        <button class="btn ghost" :disabled="radarLoading || !result?.analysisId" @click="runJdRadarFromCurrent">
          {{ radarLoading ? "运行中..." : "运行雷达分析" }}
        </button>
        <p class="message">{{ radarMessage }}</p>
        <div v-if="jdRadar">
          <p>综合得分：<strong>{{ formatScore(jdRadar.overallScore) }}</strong></p>
          <ul class="plain-list">
            <li v-for="(dim, idx) in jdRadar.dimensions || []" :key="`dim-${idx}`">{{ dim.dimension }}: {{ formatScore(dim.score) }}</li>
          </ul>
        </div>
      </article>

      <article class="panel-card">
        <h3>6) 真实性审计</h3>
        <button class="btn ghost" :disabled="auditLoading || !result?.analysisId" @click="runAuditFromCurrent">
          {{ auditLoading ? "运行中..." : "运行审计" }}
        </button>
        <p class="message">{{ auditMessage }}</p>
        <div v-if="resumeAudit">
          <p>风险等级：<strong>{{ resumeAudit.riskLevel || "-" }}</strong></p>
          <p>风险分：<strong>{{ formatScore(resumeAudit.riskScore) }}</strong></p>
          <p>{{ resumeAudit.summary || "-" }}</p>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { formatCoverage, formatScore, statusClass, statusText } from "../utils/displayFormat";

defineProps({
  analyzeForm: { type: Object, required: true },
  analyzeLoading: { type: Boolean, required: true },
  analyzeMessage: { type: String, required: true },
  queueJob: { type: Object, required: true },
  result: { type: Object, default: null },
  onFileChange: { type: Function, required: true },
  onJdImageChange: { type: Function, required: true },
  submitAnalyze: { type: Function, required: true },
  refreshCurrentJob: { type: Function, required: true },
  copyText: { type: Function, required: true },
  downloadText: { type: Function, required: true },
  resumeGenForm: { type: Object, required: true },
  resumeGenLoading: { type: Boolean, required: true },
  resumeGenMessage: { type: String, required: true },
  generatedResume: { type: Object, required: true },
  generateResumeFromJd: { type: Function, required: true },
  rewriteResumeFromCurrentAnalysis: { type: Function, required: true },
  copyGeneratedResume: { type: Function, required: true },
  downloadGeneratedResume: { type: Function, required: true },
  chatLoading: { type: Boolean, required: true },
  chatMessage: { type: String, required: true },
  chatState: { type: Object, required: true },
  startResumeChat: { type: Function, required: true },
  sendChatMessage: { type: Function, required: true },
  quickAsk: { type: Function, required: true },
  radarLoading: { type: Boolean, required: true },
  radarMessage: { type: String, required: true },
  jdRadar: { type: Object, default: null },
  runJdRadarFromCurrent: { type: Function, required: true },
  auditLoading: { type: Boolean, required: true },
  auditMessage: { type: String, required: true },
  resumeAudit: { type: Object, default: null },
  runAuditFromCurrent: { type: Function, required: true }
});

// 统一聊天角色展示文案，避免直接暴露后端英文角色名。
function chatRoleText(role) {
  const value = String(role || "").toLowerCase();
  if (value === "assistant") return "助手";
  if (value === "user") return "用户";
  if (value === "system") return "系统";
  return role || "-";
}
</script>
