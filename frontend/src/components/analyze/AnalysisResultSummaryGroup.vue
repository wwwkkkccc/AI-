<template>
  <div class="sub-card">
    <h3>分析结果</h3>
    <div class="metrics">
      <div><span>匹配评分</span><strong>{{ formatScore(result?.score) }}</strong></div>
      <div><span>关键词覆盖率</span><strong>{{ formatCoverage(result?.coverage) }}</strong></div>
    </div>

    <h3>已匹配关键词</h3>
    <p>{{ (result?.matchedKeywords || []).join(", ") || "-" }}</p>
    <h3>缺失关键词</h3>
    <p>{{ (result?.missingKeywords || []).join(", ") || "-" }}</p>
    <h3>优化摘要</h3>
    <p>{{ result?.optimized?.summary || "-" }}</p>

    <h3>重写经历（STAR）</h3>
    <ul>
      <li v-for="(line, idx) in result?.optimized?.rewrittenExperience || []" :key="`star-${idx}`">{{ line }}</li>
    </ul>

    <h3>可能面试问题</h3>
    <ul>
      <li v-for="(q, idx) in result?.optimized?.interviewQuestions || []" :key="`q-${idx}`">{{ q }}</li>
    </ul>

    <div class="sub-card">
      <h3>完整简历导出（当前分析自动生成）</h3>
      <div class="inline">
        <button
          type="button"
          class="mini-btn neutral"
          :disabled="!result?.optimizedResumeMarkdown"
          @click="$emit('copy-resume')"
        >
          复制
        </button>
        <button
          type="button"
          class="mini-btn neutral"
          :disabled="!result?.optimizedResumeMarkdown"
          @click="$emit('download-resume')"
        >
          导出 .md
        </button>
      </div>
      <pre v-if="result?.optimizedResumeMarkdown" class="md-preview">{{ result.optimizedResumeMarkdown }}</pre>
    </div>
  </div>
</template>

<script setup>
defineProps({
  result: { type: Object, required: true }
});

defineEmits(["copy-resume", "download-resume"]);

function asNumber(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

function formatScore(value) {
  return asNumber(value).toFixed(2);
}

function formatCoverage(value) {
  const n = Math.max(0, Math.min(100, asNumber(value)));
  const rounded = Math.round(n * 100) / 100;
  return `${rounded}%`;
}
</script>
