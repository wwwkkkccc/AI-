<template>
  <div class="sub-card">
    <h3>简历内容真实性/夸大检测</h3>
    <button type="button" :disabled="loading || !canRun" @click="$emit('run')">
      {{ loading ? "检测中..." : "开始检测" }}
    </button>
    <p class="message">{{ message }}</p>
    <div v-if="data">
      <div class="inline">
        <span class="state-pill" :class="auditLevelClass(data.riskLevel)">风险等级：{{ data.riskLevel }}</span>
        <span class="state-pill state-no">风险分：{{ formatScore(data.riskScore) }}</span>
      </div>
      <p>{{ data.summary }}</p>
      <ul>
        <li v-for="(item, idx) in data.auditItems || []" :key="`audit-${idx}`">
          [{{ item.severity }}] {{ item.category }} - {{ item.description }}；建议：{{ item.suggestion }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
defineProps({
  loading: { type: Boolean, default: false },
  message: { type: String, default: "" },
  data: { type: Object, default: null },
  canRun: { type: Boolean, default: false }
});

defineEmits(["run"]);

function asNumber(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

function formatScore(value) {
  return asNumber(value).toFixed(2);
}

function auditLevelClass(level) {
  const v = String(level || "").toUpperCase();
  if (v === "HIGH") return "audit-high";
  if (v === "MEDIUM") return "audit-mid";
  return "audit-low";
}
</script>
