<template>
  <div class="sub-card">
    <h3>岗位推荐</h3>

    <!-- 输入区 -->
    <div v-if="!recommendations.length" class="recommend-input">
      <form @submit.prevent="$emit('recommend')">
        <label>简历文本</label>
        <textarea
          v-model.trim="inputState.resumeText"
          rows="6"
          placeholder="粘贴简历文本，或留空使用当前分析结果"
        />

        <div class="input-row">
          <label>
            <input type="checkbox" v-model="inputState.useHistory" />
            使用历史分析结果
          </label>
        </div>

        <button :disabled="loading">
          {{ loading ? '分析中...' : '获取岗位推荐' }}
        </button>
      </form>
    </div>

    <!-- 推荐结果 -->
    <div v-else class="recommend-results">
      <div class="results-header">
        <span class="pill">找到 {{ recommendations.length }} 个匹配岗位</span>
        <button class="mini-btn" @click="$emit('reset')">重新推荐</button>
      </div>

      <div class="job-cards">
        <article v-for="job in recommendations" :key="job.id" class="job-card">
          <div class="job-header">
            <h4>{{ job.jobTitle }}</h4>
            <span class="match-score" :class="matchLevelClass(job.matchScore)">
              {{ job.matchScore }}% 匹配
            </span>
          </div>

          <div class="match-bar">
            <div class="match-fill" :style="{ width: job.matchScore + '%' }"></div>
          </div>

          <div class="skills-section">
            <div v-if="job.matchedSkills?.length" class="skill-group">
              <strong>匹配技能</strong>
              <div class="skill-tags">
                <span v-for="skill in job.matchedSkills" :key="skill" class="skill-tag matched">
                  {{ skill }}
                </span>
              </div>
            </div>

            <div v-if="job.missingSkills?.length" class="skill-group">
              <strong>缺失技能</strong>
              <div class="skill-tags">
                <span v-for="skill in job.missingSkills" :key="skill" class="skill-tag missing">
                  {{ skill }}
                </span>
              </div>
            </div>
          </div>

          <div v-if="job.reason" class="job-reason">
            <strong>推荐理由</strong>
            <p>{{ job.reason }}</p>
          </div>
        </article>
      </div>
    </div>

    <p class="message">{{ message }}</p>
  </div>
</template>

<script setup>
defineProps({
  inputState: { type: Object, required: true },
  recommendations: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  message: { type: String, default: "" }
});

defineEmits(["recommend", "reset"]);

function matchLevelClass(score) {
  const n = Number(score) || 0;
  if (n >= 80) return "match-high";
  if (n >= 60) return "match-mid";
  return "match-low";
}
</script>

<style scoped>
.recommend-input form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.input-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.job-cards {
  display: grid;
  gap: 1rem;
}

.job-card {
  padding: 1.5rem;
  background: #f9fafb;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
}

.job-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.job-header h4 {
  margin: 0;
  color: #111827;
}

.match-score {
  padding: 0.25rem 0.75rem;
  border-radius: 1rem;
  font-weight: 600;
  font-size: 0.875rem;
}

.match-high {
  background: #d1fae5;
  color: #065f46;
}

.match-mid {
  background: #fef3c7;
  color: #92400e;
}

.match-low {
  background: #fee2e2;
  color: #991b1b;
}

.match-bar {
  height: 0.5rem;
  background: #e5e7eb;
  border-radius: 0.25rem;
  overflow: hidden;
  margin-bottom: 1rem;
}

.match-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981, #3b82f6);
  transition: width 0.3s ease;
}

.skills-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1rem;
}

.skill-group strong {
  display: block;
  margin-bottom: 0.5rem;
  color: #374151;
  font-size: 0.875rem;
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.skill-tag {
  padding: 0.25rem 0.75rem;
  border-radius: 0.25rem;
  font-size: 0.875rem;
  font-weight: 500;
}

.skill-tag.matched {
  background: #d1fae5;
  color: #065f46;
}

.skill-tag.missing {
  background: #fee2e2;
  color: #991b1b;
}

.job-reason {
  padding-top: 1rem;
  border-top: 1px solid #e5e7eb;
}

.job-reason strong {
  display: block;
  margin-bottom: 0.5rem;
  color: #374151;
  font-size: 0.875rem;
}

.job-reason p {
  margin: 0;
  color: #6b7280;
  line-height: 1.5;
}
</style>
