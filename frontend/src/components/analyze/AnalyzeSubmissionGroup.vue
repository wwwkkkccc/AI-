<template>
  <form @submit.prevent="$emit('submit-analyze')">
    <label>目标岗位</label>
    <input v-model.trim="form.targetRole" type="text" placeholder="例如：后端开发工程师" />

    <label>简历文件（pdf/docx/txt）</label>
    <input
      type="file"
      accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg,.bmp,.webp,.tif,.tiff"
      @change="$emit('file-change', $event)"
      required
    />

    <label>岗位描述（JD，可直接粘贴）</label>
    <textarea v-model.trim="form.jdText" rows="8" placeholder="可粘贴JD文本；如果无法复制，可上传JD截图" />

    <label>岗位JD图片（可选，支持 png/jpg/jpeg/webp）</label>
    <input
      type="file"
      accept=".png,.jpg,.jpeg,.bmp,.webp,.tif,.tiff"
      @change="$emit('jd-image-change', $event)"
    />

    <button :disabled="loading">{{ loading ? "提交中..." : "提交分析任务" }}</button>
  </form>
  <p class="message">{{ message }}</p>

  <div v-if="queueJob?.jobId" class="queue-panel">
    <h3>当前任务</h3>
    <div class="queue-grid">
      <div>
        <span>任务ID</span>
        <strong>{{ queueJob.jobId }}</strong>
      </div>
      <div>
        <span>状态</span>
        <strong :class="statusClass(queueJob.status)">{{ statusText(queueJob.status) }}</strong>
      </div>
      <div>
        <span>排队位置</span>
        <strong>{{ queueJob.queuePosition ?? "-" }}</strong>
      </div>
      <div>
        <span>优先级</span>
        <strong>{{ queueJob.vipPriority ? "VIP优先" : "普通队列" }}</strong>
      </div>
    </div>
    <button class="mini-btn" @click="$emit('refresh-job')" :disabled="!queueJob?.jobId">手动刷新状态</button>
  </div>
</template>

<script setup>
defineProps({
  form: { type: Object, required: true },
  loading: { type: Boolean, default: false },
  message: { type: String, default: "" },
  queueJob: { type: Object, required: true },
  statusText: { type: Function, required: true },
  statusClass: { type: Function, required: true }
});

defineEmits(["submit-analyze", "file-change", "jd-image-change", "refresh-job"]);
</script>
