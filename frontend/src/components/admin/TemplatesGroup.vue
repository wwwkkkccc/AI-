<template>
  <div class="sub-card">
    <h3>简历模板管理</h3>

    <!-- 模板列表 -->
    <div v-if="!editingTemplate" class="templates-view">
      <div class="templates-header">
        <button class="mini-btn" @click="$emit('create')">
          创建新模板
        </button>
      </div>

      <div v-if="templates.length" class="templates-grid">
        <article v-for="tpl in templates" :key="tpl.id" class="template-card">
          <div class="template-preview">
            <h4>{{ tpl.name }}</h4>
            <p class="template-desc">{{ tpl.description || '暂无描述' }}</p>
            <div class="template-meta">
              <span class="pill">{{ tpl.category || '通用' }}</span>
              <span class="pill">{{ tpl.language || 'zh-CN' }}</span>
            </div>
          </div>
          <div class="template-actions">
            <button class="mini-btn" @click="$emit('preview', tpl.id)">预览</button>
            <button class="mini-btn" @click="$emit('edit', tpl.id)">编辑</button>
            <button class="mini-btn warn" @click="$emit('delete', tpl.id)">删除</button>
          </div>
        </article>
      </div>

      <p v-else class="empty-hint">暂无模板</p>
    </div>

    <!-- 创建/编辑表单 -->
    <div v-else class="template-form">
      <div class="form-header">
        <h4>{{ editingTemplate.id ? '编辑模板' : '创建模板' }}</h4>
        <button class="mini-btn" @click="$emit('cancel')">取消</button>
      </div>

      <form @submit.prevent="$emit('save')">
        <div class="form-field">
          <label>模板名称</label>
          <input v-model.trim="editingTemplate.name" type="text" required placeholder="例如：技术岗位通用模板" />
        </div>

        <div class="form-field">
          <label>模板描述</label>
          <textarea v-model.trim="editingTemplate.description" rows="3" placeholder="简要描述模板用途" />
        </div>

        <div class="form-row">
          <div class="form-field">
            <label>分类</label>
            <select v-model="editingTemplate.category">
              <option value="通用">通用</option>
              <option value="技术">技术</option>
              <option value="管理">管理</option>
              <option value="销售">销售</option>
              <option value="设计">设计</option>
            </select>
          </div>

          <div class="form-field">
            <label>语言</label>
            <select v-model="editingTemplate.language">
              <option value="zh-CN">中文</option>
              <option value="en-US">英文</option>
            </select>
          </div>
        </div>

        <div class="form-field">
          <label>模板内容（Markdown 格式）</label>
          <textarea v-model.trim="editingTemplate.content" rows="15" required placeholder="使用 Markdown 编写模板内容" />
        </div>

        <div class="form-actions">
          <button type="submit" :disabled="loading">
            {{ loading ? '保存中...' : '保存模板' }}
          </button>
          <button type="button" class="mini-btn" @click="$emit('cancel')">
            取消
          </button>
        </div>
      </form>
    </div>

    <!-- 预览模态框 -->
    <div v-if="previewingTemplate" class="modal-overlay" @click="$emit('close-preview')">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h4>{{ previewingTemplate.name }}</h4>
          <button class="close-btn" @click="$emit('close-preview')">×</button>
        </div>
        <div class="modal-body">
          <div class="markdown-preview" v-html="renderMarkdown(previewingTemplate.content)"></div>
        </div>
      </div>
    </div>

    <p class="message">{{ message }}</p>
  </div>
</template>

<script setup>
defineProps({
  templates: { type: Array, default: () => [] },
  editingTemplate: { type: Object, default: null },
  previewingTemplate: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  message: { type: String, default: "" }
});

defineEmits(["create", "edit", "delete", "preview", "close-preview", "save", "cancel"]);

function renderMarkdown(text) {
  if (!text) return "";
  // 简单的 Markdown 渲染
  return text
    .replace(/^### (.+)$/gm, "<h3>$1</h3>")
    .replace(/^## (.+)$/gm, "<h2>$1</h2>")
    .replace(/^# (.+)$/gm, "<h1>$1</h1>")
    .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
    .replace(/\*(.+?)\*/g, "<em>$1</em>")
    .replace(/^- (.+)$/gm, "<li>$1</li>")
    .replace(/\n/g, "<br>");
}
</script>

<style scoped>
.templates-header {
  margin-bottom: 1.5rem;
  display: flex;
  justify-content: flex-end;
}

.templates-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}

.template-card {
  padding: 1.5rem;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s, box-shadow 0.2s;
}

.template-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.template-preview h4 {
  margin: 0 0 0.5rem 0;
  color: #111827;
}

.template-desc {
  color: #6b7280;
  font-size: 0.875rem;
  margin-bottom: 1rem;
}

.template-meta {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.template-actions {
  display: flex;
  gap: 0.5rem;
  padding-top: 1rem;
  border-top: 1px solid #e5e7eb;
}

.empty-hint {
  text-align: center;
  color: #6b7280;
  padding: 2rem;
}

.template-form {
  max-width: 800px;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #e5e7eb;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.form-field label {
  font-weight: 500;
  color: #374151;
}

.form-field input,
.form-field textarea,
.form-field select {
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 0.25rem;
  font-family: inherit;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-actions {
  display: flex;
  gap: 1rem;
  margin-top: 1.5rem;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 0.5rem;
  max-width: 800px;
  max-height: 80vh;
  width: 90%;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h4 {
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 2rem;
  color: #6b7280;
  cursor: pointer;
  line-height: 1;
}

.close-btn:hover {
  color: #111827;
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
}

.markdown-preview {
  line-height: 1.6;
}

.markdown-preview :deep(h1),
.markdown-preview :deep(h2),
.markdown-preview :deep(h3) {
  margin-top: 1rem;
  margin-bottom: 0.5rem;
  color: #111827;
}

.markdown-preview :deep(li) {
  margin-left: 1.5rem;
}
</style>
