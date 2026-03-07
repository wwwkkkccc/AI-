<template>
  <section class="stack">
    <article class="panel-card">
      <h3>1) Submit Analysis Job</h3>
      <form class="form-grid" @submit.prevent="submitAnalyze">
        <label class="field">
          <span>Target Role</span>
          <input v-model.trim="analyzeForm.targetRole" type="text" placeholder="e.g. Senior Backend Engineer" />
        </label>
        <label class="field">
          <span>Resume File</span>
          <input
            type="file"
            accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg,.bmp,.webp,.tif,.tiff"
            @change="onFileChange"
            required
          />
        </label>
        <label class="field">
          <span>JD Text</span>
          <textarea v-model.trim="analyzeForm.jdText" rows="5" />
        </label>
        <label class="field">
          <span>JD Image (Optional)</span>
          <input type="file" accept=".png,.jpg,.jpeg,.bmp,.webp,.tif,.tiff" @change="onJdImageChange" />
        </label>

        <div class="actions">
          <button class="btn primary" :disabled="analyzeLoading" type="submit">
            {{ analyzeLoading ? "Submitting..." : "Submit Analysis" }}
          </button>
          <button class="btn ghost" type="button" :disabled="!queueJob.jobId" @click="refreshCurrentJob">Refresh Job</button>
        </div>
      </form>

      <p class="message">{{ analyzeMessage }}</p>
      <div v-if="queueJob.jobId" class="queue-grid">
        <div><span>Job ID</span><strong>{{ queueJob.jobId }}</strong></div>
        <div><span>Status</span><strong :class="statusClass(queueJob.status)">{{ statusText(queueJob.status) }}</strong></div>
        <div><span>Queue Position</span><strong>{{ queueJob.queuePosition ?? "-" }}</strong></div>
        <div><span>Priority</span><strong>{{ queueJob.vipPriority ? "VIP" : "Standard" }}</strong></div>
      </div>
    </article>

    <article v-if="result" class="panel-card">
      <h3>2) Analysis Result</h3>
      <div class="metric-grid">
        <div class="metric"><span>Score</span><strong>{{ formatScore(result.score) }}</strong></div>
        <div class="metric"><span>Coverage</span><strong>{{ formatCoverage(result.coverage) }}</strong></div>
      </div>

      <p><strong>Matched Keywords:</strong> {{ (result.matchedKeywords || []).join(", ") || "-" }}</p>
      <p><strong>Missing Keywords:</strong> {{ (result.missingKeywords || []).join(", ") || "-" }}</p>
      <p><strong>Summary:</strong> {{ result.optimized?.summary || "-" }}</p>

      <div class="actions">
        <button class="btn ghost" :disabled="!result.optimizedResumeMarkdown" @click="copyText(result.optimizedResumeMarkdown)">
          Copy Optimized Resume
        </button>
        <button
          class="btn ghost"
          :disabled="!result.optimizedResumeMarkdown"
          @click="downloadText('optimized-resume.md', result.optimizedResumeMarkdown)"
        >
          Download .md
        </button>
      </div>

      <pre v-if="result.optimizedResumeMarkdown" class="code-preview">{{ result.optimizedResumeMarkdown }}</pre>
    </article>

    <article class="panel-card">
      <h3>3) Generate / Rewrite</h3>
      <form class="form-grid" @submit.prevent="generateResumeFromJd">
        <label class="field"><span>Target Role</span><input v-model.trim="resumeGenForm.targetRole" type="text" required /></label>
        <label class="field"><span>JD Text</span><textarea v-model.trim="resumeGenForm.jdText" rows="4" required /></label>
        <label class="field"><span>Background (Optional)</span><textarea v-model.trim="resumeGenForm.userBackground" rows="3" /></label>

        <div class="actions">
          <button class="btn primary" :disabled="resumeGenLoading" type="submit">
            {{ resumeGenLoading ? "Generating..." : "Generate from JD" }}
          </button>
          <button class="btn ghost" type="button" :disabled="resumeGenLoading || !result?.analysisId" @click="rewriteResumeFromCurrentAnalysis">
            Rewrite from Current Analysis
          </button>
        </div>
      </form>

      <p class="message">{{ resumeGenMessage }}</p>
      <div v-if="generatedResume.markdown">
        <div class="actions">
          <button class="btn ghost" @click="copyGeneratedResume">Copy</button>
          <button class="btn ghost" @click="downloadGeneratedResume">Download</button>
        </div>
        <pre class="code-preview">{{ generatedResume.markdown }}</pre>
      </div>
    </article>

    <article class="panel-card">
      <h3>4) Chat Optimization</h3>
      <div class="actions">
        <button class="btn ghost" :disabled="chatLoading || !result?.analysisId" @click="startResumeChat">Start Session</button>
        <button class="btn ghost" :disabled="chatLoading || !chatState.sessionId" @click="quickAsk('Give me 3 top-priority improvements')">
          Quick Ask
        </button>
      </div>
      <p class="message">{{ chatMessage }}</p>

      <div class="chat-box">
        <div v-for="msg in chatState.messages" :key="msg.id || `${msg.role}-${msg.createdAt}`" class="chat-item">
          <strong>{{ msg.role }}</strong>
          <p>{{ msg.content }}</p>
        </div>
      </div>

      <form class="inline-form" @submit.prevent="sendChatMessage">
        <input v-model.trim="chatState.input" type="text" placeholder="Type your question..." />
        <button class="btn primary" :disabled="chatLoading || !chatState.input" type="submit">Send</button>
      </form>
    </article>

    <div class="split-row">
      <article class="panel-card">
        <h3>5) JD Radar</h3>
        <button class="btn ghost" :disabled="radarLoading || !result?.analysisId" @click="runJdRadarFromCurrent">
          {{ radarLoading ? "Running..." : "Run Radar Analysis" }}
        </button>
        <p class="message">{{ radarMessage }}</p>
        <div v-if="jdRadar">
          <p>Overall: <strong>{{ formatScore(jdRadar.overallScore) }}</strong></p>
          <ul class="plain-list">
            <li v-for="(dim, idx) in jdRadar.dimensions || []" :key="`dim-${idx}`">{{ dim.dimension }}: {{ formatScore(dim.score) }}</li>
          </ul>
        </div>
      </article>

      <article class="panel-card">
        <h3>6) Authenticity Audit</h3>
        <button class="btn ghost" :disabled="auditLoading || !result?.analysisId" @click="runAuditFromCurrent">
          {{ auditLoading ? "Running..." : "Run Audit" }}
        </button>
        <p class="message">{{ auditMessage }}</p>
        <div v-if="resumeAudit">
          <p>Risk Level: <strong>{{ resumeAudit.riskLevel || "-" }}</strong></p>
          <p>Risk Score: <strong>{{ formatScore(resumeAudit.riskScore) }}</strong></p>
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
</script>

