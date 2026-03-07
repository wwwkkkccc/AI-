<template>
  <div class="sub-card">
    <h3>简历版本管理</h3>

    <!-- 版本列表 -->
    <div v-if="!viewingVersion && !comparingVersions" class="versions-list">
      <div class="versions-header">
        <button class="mini-btn" @click="$emit('save-version')">
          {{ loading ? '保存中...' : '保存当前版本' }}
        </button>
      </div>

      <table v-if="versions.length" class="data-table">
        <thead>
          <tr>
            <th>版本号</th>
            <th>标题</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="ver in versions" :key="ver.id">
            <td><span class="pill">v{{ ver.versionNumber }}</span></td>
            <td>{{ ver.title || '未命名版本' }}</td>
            <td>{{ formatDate(ver.createdAt) }}</td>
            <td class="actions">
              <button class="mini-btn" @click="$emit('view', ver.id)">查看</button>
              <button class="mini-btn" @click="$emit('compare', ver.id)">对比</button>
              <button class="mini-btn warn" @click="$emit('delete', ver.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>

      <p v-else class="empty-hint">暂无版本记录</p>
    </div>

    <!-- 版本详情查看 -->
    <div v-if="viewingVersion" class="version-detail">
      <div class="detail-header">
        <h4>版本 v{{ viewingVersion.versionNumber }} - {{ viewingVersion.title }}</h4>
        <button class="mini-btn" @click="$emit('close-view')">返回列表</button>
      </div>
      <div class="markdown-content" v-html="renderMarkdown(viewingVersion.content)"></div>
    </div>

    <!-- 版本对比 -->
    <div v-if="comparingVersions" class="version-compare">
      <div class="compare-header">
        <h4>版本对比</h4>
        <button class="mini-btn" @click="$emit('close-compare')">返回列表</button>
      </div>
      <div class="compare-grid">
        <div class="compare-col">
          <h5>版本 v{{ comparingVersions.old.versionNumber }}</h5>
          <div class="markdown-content" v-html="renderMarkdown(comparingVersions.old.content)"></div>
        </div>
        <div class="compare-col">
          <h5>版本 v{{ comparingVersions.new.versionNumber }}</h5>
          <div class="markdown-content" v-html="renderMarkdown(comparingVersions.new.content)"></div>
        </div>
      </div>
    </div>

    <p class="message">{{ message }}</p>
  </div>
</template>

<script setup>
defineProps({
  versions: { type: Array, default: () => [] },
  viewingVersion: { type: Object, default: null },
  comparingVersions: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  message: { type: String, default: "" }
});

defineEmits(["save-version", "view", "compare", "delete", "close-view", "close-compare"]);

function formatDate(dateStr) {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  return d.toLocaleString("zh-CN");
}

function renderMarkdown(text) {
  if (!text) return "";
  // 简单的 Markdown 渲染
  return text
    .replace(/^### (.+)$/gm, "<h3>$1</h3>")
    .replace(/^## (.+)$/gm, "<h2>$1</h2>")
    .replace(/^# (.+)$/gm, "<h1>$1</h1>")
    .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
    .replace(/\*(.+?)\*/g, "<em>$1</em>")
    .replace(/\n/g, "<br>");
}
</script>

<style scoped>
.versions-header {
  margin-bottom: 1rem;
  display: flex;
  justify-content: flex-end;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 1rem;
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

.data-table .actions {
  display: flex;
  gap: 0.5rem;
}

.empty-hint {
  text-align: center;
  color: #6b7280;
  padding: 2rem;
}

.detail-header,
.compare-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #e5e7eb;
}

.markdown-content {
  padding: 1rem;
  background: #f9fafb;
  border-radius: 0.5rem;
  line-height: 1.6;
}

.compare-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.compare-col h5 {
  margin-bottom: 0.5rem;
  color: #374151;
}
</style>
