<template>
  <div class="sub-card">
    <h3>JD 智能解析与岗位匹配度雷达图</h3>
    <button type="button" :disabled="loading || !canRun" @click="$emit('run')">
      {{ loading ? "解析中..." : "生成雷达图" }}
    </button>
    <p class="message">{{ message }}</p>
    <div v-if="data" class="radar-wrap">
      <svg viewBox="0 0 320 320" class="radar-svg">
        <polygon :points="radarMaxPolygon(data.dimensions || [])" class="radar-max" />
        <polygon :points="radarValuePolygon(data.dimensions || [])" class="radar-value" />
        <line
          v-for="(d, idx) in data.dimensions || []"
          :key="`axis-${idx}`"
          x1="160"
          y1="160"
          :x2="radarAxisX(idx, (data.dimensions || []).length)"
          :y2="radarAxisY(idx, (data.dimensions || []).length)"
          class="radar-axis"
        />
        <text
          v-for="(d, idx) in data.dimensions || []"
          :key="`label-${idx}`"
          :x="radarLabelX(idx, (data.dimensions || []).length)"
          :y="radarLabelY(idx, (data.dimensions || []).length)"
          class="radar-label"
        >
          {{ d.name }}
        </text>
      </svg>
      <div class="radar-detail">
        <p class="message">综合匹配度：{{ formatScore(data.overallScore) }}</p>
        <ul>
          <li v-for="(d, idx) in data.dimensions || []" :key="`dim-${idx}`">
            {{ d.name }}：{{ formatScore(d.score) }} / {{ formatScore(d.maxScore) }}，{{ d.detail }}
          </li>
        </ul>
      </div>
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

function radarPolygon(scores, count, radius = 110, cx = 160, cy = 160) {
  if (!count) return "";
  const points = [];
  for (let i = 0; i < count; i += 1) {
    const angle = (Math.PI * 2 * i) / count - Math.PI / 2;
    const score = Math.max(0, Math.min(100, asNumber(scores[i])));
    const r = (radius * score) / 100;
    const x = cx + Math.cos(angle) * r;
    const y = cy + Math.sin(angle) * r;
    points.push(`${x},${y}`);
  }
  return points.join(" ");
}

function radarMaxPolygon(dimensions) {
  const count = (dimensions || []).length;
  return radarPolygon(new Array(count).fill(100), count);
}

function radarValuePolygon(dimensions) {
  const rows = dimensions || [];
  return radarPolygon(rows.map((d) => d?.score ?? 0), rows.length);
}

function radarAxisX(idx, count, radius = 110, cx = 160) {
  const angle = (Math.PI * 2 * idx) / Math.max(1, count) - Math.PI / 2;
  return cx + Math.cos(angle) * radius;
}

function radarAxisY(idx, count, radius = 110, cy = 160) {
  const angle = (Math.PI * 2 * idx) / Math.max(1, count) - Math.PI / 2;
  return cy + Math.sin(angle) * radius;
}

function radarLabelX(idx, count, radius = 130, cx = 160) {
  const angle = (Math.PI * 2 * idx) / Math.max(1, count) - Math.PI / 2;
  return cx + Math.cos(angle) * radius;
}

function radarLabelY(idx, count, radius = 130, cy = 160) {
  const angle = (Math.PI * 2 * idx) / Math.max(1, count) - Math.PI / 2;
  return cy + Math.sin(angle) * radius;
}
</script>
