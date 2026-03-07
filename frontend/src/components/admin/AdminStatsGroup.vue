<template>
  <div class="sub-card">
    <h3>系统统计</h3>

    <!-- 概览卡片 -->
    <div class="admin-stats-overview">
      <div class="admin-stat-card">
        <span class="stat-icon">👥</span>
        <div class="stat-info">
          <span class="stat-label">总用户数</span>
          <span class="stat-value">{{ stats.totalUsers || 0 }}</span>
        </div>
      </div>
      <div class="admin-stat-card">
        <span class="stat-icon">✅</span>
        <div class="stat-info">
          <span class="stat-label">活跃用户</span>
          <span class="stat-value">{{ stats.activeUsers || 0 }}</span>
        </div>
      </div>
      <div class="admin-stat-card">
        <span class="stat-icon">📊</span>
        <div class="stat-info">
          <span class="stat-label">总分析次数</span>
          <span class="stat-value">{{ stats.totalAnalyses || 0 }}</span>
        </div>
      </div>
      <div class="admin-stat-card">
        <span class="stat-icon">⭐</span>
        <div class="stat-info">
          <span class="stat-label">平均分数</span>
          <span class="stat-value">{{ formatScore(stats.avgScore) }}</span>
        </div>
      </div>
    </div>

    <!-- 每日分析量柱状图 -->
    <div v-if="stats.dailyAnalyses?.length" class="chart-section">
      <h4>每日分析量（最近 30 天）</h4>
      <svg class="bar-chart" viewBox="0 0 800 300" xmlns="http://www.w3.org/2000/svg">
        <!-- 网格线 -->
        <line v-for="i in 6" :key="`grid-${i}`" x1="50" :y1="i * 50" x2="780" :y2="i * 50" stroke="#e5e7eb" stroke-width="1" />

        <!-- 柱状图 -->
        <rect
          v-for="(day, idx) in stats.dailyAnalyses"
          :key="`bar-${idx}`"
          :x="50 + idx * 25"
          :y="300 - (day.count * 2)"
          width="20"
          :height="day.count * 2"
          fill="#3b82f6"
          opacity="0.8"
        />

        <!-- Y 轴标签 -->
        <text v-for="i in 6" :key="`label-${i}`" x="10" :y="305 - i * 50" font-size="12" fill="#6b7280">
          {{ (i - 1) * 50 }}
        </text>
      </svg>
    </div>

    <!-- 热门岗位列表 -->
    <div v-if="stats.topJobs?.length" class="top-jobs-section">
      <h4>热门岗位 (Top 10)</h4>
      <table class="data-table">
        <thead>
          <tr>
            <th>排名</th>
            <th>岗位名称</th>
            <th>分析次数</th>
            <th>平均分数</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(job, idx) in stats.topJobs" :key="job.jobTitle">
            <td><span class="rank-badge">{{ idx + 1 }}</span></td>
            <td>{{ job.jobTitle }}</td>
            <td>{{ job.count }}</td>
            <td>
              <span class="score-badge" :class="scoreLevelClass(job.avgScore)">
                {{ formatScore(job.avgScore) }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <p class="message">{{ message }}</p>
  </div>
</template>

<script setup>
defineProps({
  stats: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false },
  message: { type: String, default: "" }
});

function formatScore(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n.toFixed(1) : "0.0";
}

function scoreLevelClass(score) {
  const n = Number(score) || 0;
  if (n >= 80) return "score-high";
  if (n >= 60) return "score-mid";
  return "score-low";
}
</script>

<style scoped>
.admin-stats-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.admin-stat-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  font-size: 2.5rem;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 0.25rem;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: #111827;
}

.chart-section,
.top-jobs-section {
  margin-top: 2rem;
}

.chart-section h4,
.top-jobs-section h4 {
  margin-bottom: 1rem;
  color: #374151;
}

.bar-chart {
  width: 100%;
  height: auto;
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  background: #f9fafb;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.data-table th {
  background: #f9fafb;
  font-weight: 600;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  background: #3b82f6;
  color: white;
  border-radius: 50%;
  font-weight: 600;
}

.score-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 0.25rem;
  font-weight: 600;
  font-size: 0.875rem;
}

.score-high {
  background: #d1fae5;
  color: #065f46;
}

.score-mid {
  background: #fef3c7;
  color: #92400e;
}

.score-low {
  background: #fee2e2;
  color: #991b1b;
}
</style>
