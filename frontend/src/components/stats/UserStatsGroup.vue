<template>
  <div class="sub-card">
    <h3>我的统计</h3>

    <!-- 概览卡片 -->
    <div class="stats-overview">
      <div class="stat-card">
        <span class="stat-label">总分析次数</span>
        <span class="stat-value">{{ stats.totalAnalyses || 0 }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">平均分数</span>
        <span class="stat-value">{{ formatScore(stats.avgScore) }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">最高分数</span>
        <span class="stat-value">{{ formatScore(stats.maxScore) }}</span>
      </div>
    </div>

    <!-- 分数趋势图 -->
    <div v-if="stats.scoreTrend?.length" class="chart-section">
      <h4>分数趋势</h4>
      <svg class="trend-chart" viewBox="0 0 600 200" xmlns="http://www.w3.org/2000/svg">
        <!-- 网格线 -->
        <line v-for="i in 5" :key="`grid-${i}`" x1="50" :y1="i * 40" x2="580" :y2="i * 40" stroke="#e5e7eb" stroke-width="1" />

        <!-- 趋势线 -->
        <polyline
          :points="trendPoints"
          fill="none"
          stroke="#3b82f6"
          stroke-width="2"
        />

        <!-- 数据点 -->
        <circle
          v-for="(point, idx) in stats.scoreTrend"
          :key="`point-${idx}`"
          :cx="50 + idx * (530 / (stats.scoreTrend.length - 1))"
          :cy="200 - (point.score * 1.8)"
          r="4"
          fill="#3b82f6"
        />

        <!-- Y 轴标签 -->
        <text v-for="i in 6" :key="`label-${i}`" x="10" :y="205 - i * 40" font-size="12" fill="#6b7280">
          {{ (i - 1) * 20 }}
        </text>
      </svg>
    </div>

    <!-- 关键词云 -->
    <div v-if="stats.topKeywords?.length" class="keywords-section">
      <h4>匹配最多的关键词 (Top 10)</h4>
      <div class="keyword-cloud">
        <span
          v-for="kw in stats.topKeywords"
          :key="kw.keyword"
          class="keyword-tag"
          :style="{ fontSize: keywordSize(kw.count) }"
        >
          {{ kw.keyword }} ({{ kw.count }})
        </span>
      </div>
    </div>

    <div v-if="stats.missingKeywords?.length" class="keywords-section">
      <h4>缺失最多的关键词 (Top 10)</h4>
      <div class="keyword-cloud missing">
        <span
          v-for="kw in stats.missingKeywords"
          :key="kw.keyword"
          class="keyword-tag"
          :style="{ fontSize: keywordSize(kw.count) }"
        >
          {{ kw.keyword }} ({{ kw.count }})
        </span>
      </div>
    </div>

    <p class="message">{{ message }}</p>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  stats: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false },
  message: { type: String, default: "" }
});

function formatScore(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n.toFixed(1) : "0.0";
}

function keywordSize(count) {
  const base = 0.875;
  const scale = Math.min(count / 10, 2);
  return `${base + scale * 0.5}rem`;
}

const trendPoints = computed(() => {
  if (!props.stats.scoreTrend?.length) return "";
  return props.stats.scoreTrend
    .map((point, idx) => {
      const x = 50 + idx * (530 / (props.stats.scoreTrend.length - 1));
      const y = 200 - (point.score * 1.8);
      return `${x},${y}`;
    })
    .join(" ");
});
</script>

<style scoped>
.stats-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-card {
  padding: 1.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 0.5rem;
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 0.875rem;
  opacity: 0.9;
  margin-bottom: 0.5rem;
}

.stat-value {
  display: block;
  font-size: 2rem;
  font-weight: 700;
}

.chart-section,
.keywords-section {
  margin-top: 2rem;
}

.chart-section h4,
.keywords-section h4 {
  margin-bottom: 1rem;
  color: #374151;
}

.trend-chart {
  width: 100%;
  height: auto;
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  background: #f9fafb;
}

.keyword-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  padding: 1rem;
  background: #f9fafb;
  border-radius: 0.5rem;
}

.keyword-tag {
  padding: 0.5rem 1rem;
  background: #dbeafe;
  color: #1e40af;
  border-radius: 0.25rem;
  font-weight: 500;
  transition: transform 0.2s;
}

.keyword-tag:hover {
  transform: scale(1.05);
}

.keyword-cloud.missing .keyword-tag {
  background: #fee2e2;
  color: #991b1b;
}
</style>
