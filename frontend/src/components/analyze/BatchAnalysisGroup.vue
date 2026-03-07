<template>
  <div class="sub-card">
    <h3>批量分析</h3>

    <!-- 上传表单 -->
    <div v-if="!batchState.jobs.length" class="batch-form">
      <form @submit.prevent="$emit('submit')">
        <label>选择多个简历文件</label>
        <input
          type="file"
          multiple
          accept=".pdf,.docx,.doc,.txt,.png,.jpg,.jpeg"
          @change="$emit('files-change', $event)"
          required
        />

        <label>岗位描述 JD</label>
        <textarea
          v-model.trim="batchState.jdText"
          rows="4"
          placeholder="粘贴岗位描述"
          required
        />

        <label>目标岗位</label>
        <input
          v-model.trim="batchState.targetRole"
          type="text"
          placeholder="例如：Java 高级后端工程师"
          required
        />

        <button :disabled="loading">
          {{ loading ? '提交中...' : '开始批量分析' }}
        </button>
      </form>
    </div>

    <!-- 批量进度 -->
    <div v-else class="batch-progress">
      <div class="progress-header">
        <h4>批量分析进度</h4>
        <button v-if="batchState.allDone" class="mini-btn" @click="$emit('reset')">
          重新分析
        </button>
      </div>

      <!-- 总进度条 -->
      <div class="total-progress">
        <div class="progress-info">
          <span>总进度：{{ batchState.completedCount }} / {{ batchState.jobs.length }}</span>
          <span>{{ batchState.progressPercent }}%</span>
        </div>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: batchState.progressPercent + '%' }"></div>
        </div>
      </div>

      <!-- 每个文件的状态 -->
      <div class="job-list">
        <div v-for="job in batchState.jobs" :key="job.id" class="job-item">
          <div class="job-info">
            <span class="job-name">{{ job.fileName }}</span>
            <span class="job-status" :class="statusClass(job.status)">
              {{ statusText(job.status) }}
            </span>
          </div>
          <div v-if="job.score != null" class="job-score">
            <span class="score-badge" :class="scoreLevelClass(job.score)">
              {{ job.score }} 分
            </span>
          </div>
          <div v-if="job.error" class="job-error">
            <span class="error-text">{{ job.error }}</span>
          </div>
        </div>
      </div>

      <!-- 完成后排序展示 -->
      <div v-if="batchState.allDone && batchState.sortedJobs.length" class="batch-results">
        <h4>分析结果（按分数排序）</h4>
        <table class="data-table">
          <thead>
            <tr>
              <th>排名</th>
              <th>文件名</th>
              <th>分数</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(job, idx) in batchState.sortedJobs" :key="job.id">
              <td><span class="rank-badge">{{ idx + 1 }}</span></td>
              <td>{{ job.fileName }}</td>
              <td>
                <span class="score-badge" :class="scoreLevelClass(job.score)">
                  {{ job.score }} 分
                </span>
              </td>
              <td>
                <button class="mini-btn" @click="$emit('view-result', job.id)">
                  查看详情
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <p class="message">{{ message }}</p>
  </div>
</template>

<script setup>
defineProps({
  batchState: { type: Object, required: true },
  loading: { type: Boolean, default: false },
  message: { type: String, default: "" }
});

defineEmits(["submit", "files-change", "reset", "view-result"]);

function statusClass(status) {
  const map = {
    PENDING: "status-pending",
    PROCESSING: "status-processing",
    DONE: "status-done",
    FAILED: "status-failed"
  };
  return map[status] || "";
}

function statusText(status) {
  const map = {
    PENDING: "等待中",
    PROCESSING: "处理中",
    DONE: "已完成",
    FAILED: "失败"
  };
  return map[status] || status;
}

function scoreLevelClass(score) {
  const n = Number(score) || 0;
  if (n >= 80) return "score-high";
  if (n >= 60) return "score-mid";
  return "score-low";
}
</script>

<style scoped>
.batch-form form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.total-progress {
  margin-bottom: 2rem;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
  color: #6b7280;
}

.progress-bar {
  height: 1rem;
  background: #e5e7eb;
  border-radius: 0.5rem;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #10b981);
  transition: width 0.3s ease;
}

.job-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 2rem;
}

.job-item {
  padding: 1rem;
  background: #f9fafb;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
}

.job-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.job-name {
  font-weight: 500;
  color: #111827;
}

.job-status {
  padding: 0.25rem 0.75rem;
  border-radius: 0.25rem;
  font-size: 0.875rem;
  font-weight: 500;
}

.status-pending {
  background: #e5e7eb;
  color: #6b7280;
}

.status-processing {
  background: #dbeafe;
  color: #1e40af;
}

.status-done {
  background: #d1fae5;
  color: #065f46;
}

.status-failed {
  background: #fee2e2;
  color: #991b1b;
}

.job-score {
  margin-top: 0.5rem;
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

.job-error {
  margin-top: 0.5rem;
}

.error-text {
  color: #dc2626;
  font-size: 0.875rem;
}

.batch-results {
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 2px solid #e5e7eb;
}

.batch-results h4 {
  margin-bottom: 1rem;
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
</style>
