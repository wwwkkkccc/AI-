<template>
  <div class="sub-card">
    <h3>AI 简历一键生成/重写</h3>
    <form @submit.prevent="$emit('submit-generate')">
      <label>目标岗位</label>
      <input v-model.trim="form.targetRole" type="text" placeholder="例如：高级后端工程师" />

      <label>岗位描述（JD）</label>
      <textarea v-model.trim="form.jdText" rows="6" placeholder="粘贴岗位 JD，用于生成定制化简历" />

      <label>个人背景（可选）</label>
      <textarea v-model.trim="form.userBackground" rows="4" placeholder="可补充教育、项目、擅长方向等背景信息" />

      <div class="inline">
        <button :disabled="loading">{{ loading ? "生成中..." : "从 JD 一键生成" }}</button>
        <button
          type="button"
          class="mini-btn neutral"
          :disabled="loading || !canRewrite"
          @click="$emit('rewrite')"
        >
          基于当前分析重写
        </button>
        <button
          type="button"
          class="mini-btn neutral"
          :disabled="!generatedResume?.markdown"
          @click="$emit('copy')"
        >
          复制 Markdown
        </button>
        <button
          type="button"
          class="mini-btn neutral"
          :disabled="!generatedResume?.markdown"
          @click="$emit('download')"
        >
          导出 .md
        </button>
      </div>
    </form>
    <p class="message">{{ message }}</p>
    <pre v-if="generatedResume?.markdown" class="md-preview">{{ generatedResume.markdown }}</pre>
  </div>
</template>

<script setup>
defineProps({
  form: { type: Object, required: true },
  loading: { type: Boolean, default: false },
  message: { type: String, default: "" },
  generatedResume: { type: Object, required: true },
  canRewrite: { type: Boolean, default: false }
});

defineEmits(["submit-generate", "rewrite", "copy", "download"]);
</script>
