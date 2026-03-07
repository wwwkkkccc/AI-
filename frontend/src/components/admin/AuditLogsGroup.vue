<template>
  <div class="sub-card">
    <h3>审计日志</h3>

    <!-- 筛选器 -->
    <div class="filters">
      <div class="filter-row">
        <div class="filter-field">
          <label>操作类型</label>
          <select v-model="filters.actionType">
            <option value="">全部</option>
            <option value="CREATE">创建</option>
            <option value="UPDATE">更新</option>
            <option value="DELETE">删除</option>
            <option value="LOGIN">登录</option>
            <option value="LOGOUT">登出</option>
          </select>
        </div>

        <div class="filter-field">
          <label>管理员用户名</label>
          <input v-model.trim="filters.adminUsername" type="text" placeholder="输入用户名" />
        </div>

        <div class="filter-field">
          <label>开始日期</label>
          <input v-model="filters.startDate" type="date" />
        </div>

        <div class="filter-field">
          <label>结束日期</label>
          <input v-model="filters.endDate" type="date" />
        </div>

        <button class="mini-btn" @click="$emit('filter')">
          {{ loading ? '查询中...' : '筛选' }}
        </button>
        <button class="mini-btn" @click="$emit('reset-filter')">重置</button>
      </div>
    </div>

    <!-- 日志表格 -->
    <div v-if="logs.length" class="logs-table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>时间</th>
            <th>管理员</th>
            <th>操作</th>
            <th>目标</th>
            <th>详情</th>
            <th>IP 地址</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in logs" :key="log.id">
            <td>{{ formatDate(log.createdAt) }}</td>
            <td>{{ log.adminUsername }}</td>
            <td>
              <span class="action-badge" :class="actionClass(log.actionType)">
                {{ actionText(log.actionType) }}
              </span>
            </td>
            <td>{{ log.targetType }} #{{ log.targetId }}</td>
            <td class="details-cell">{{ log.details || '-' }}</td>
            <td>{{ log.ipAddress || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <p v-else class="empty-hint">暂无审计日志</p>

    <!-- 分页控件 -->
    <div v-if="pagination.totalPages > 1" class="pagination">
      <button
        class="mini-btn"
        :disabled="pagination.currentPage === 1"
        @click="$emit('page-change', pagination.currentPage - 1)"
      >
        上一页
      </button>
      <span class="page-info">
        第 {{ pagination.currentPage }} / {{ pagination.totalPages }} 页
      </span>
      <button
        class="mini-btn"
        :disabled="pagination.currentPage === pagination.totalPages"
        @click="$emit('page-change', pagination.currentPage + 1)"
      >
        下一页
      </button>
    </div>

    <p class="message">{{ message }}</p>
  </div>
</template>

<script setup>
defineProps({
  filters: { type: Object, required: true },
  logs: { type: Array, default: () => [] },
  pagination: { type: Object, default: () => ({ currentPage: 1, totalPages: 1 }) },
  loading: { type: Boolean, default: false },
  message: { type: String, default: "" }
});

defineEmits(["filter", "reset-filter", "page-change"]);

function formatDate(dateStr) {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  return d.toLocaleString("zh-CN");
}

function actionClass(type) {
  const map = {
    CREATE: "action-create",
    UPDATE: "action-update",
    DELETE: "action-delete",
    LOGIN: "action-login",
    LOGOUT: "action-logout"
  };
  return map[type] || "";
}

function actionText(type) {
  const map = {
    CREATE: "创建",
    UPDATE: "更新",
    DELETE: "删除",
    LOGIN: "登录",
    LOGOUT: "登出"
  };
  return map[type] || type;
}
</script>

<style scoped>
.filters {
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: #f9fafb;
  border-radius: 0.5rem;
}

.filter-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 1rem;
  align-items: end;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.filter-field label {
  font-size: 0.875rem;
  color: #374151;
}

.filter-field input,
.filter-field select {
  padding: 0.5rem;
  border: 1px solid #d1d5db;
  border-radius: 0.25rem;
}

.logs-table-wrapper {
  overflow-x: auto;
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
  white-space: nowrap;
}

.details-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 0.25rem;
  font-size: 0.875rem;
  font-weight: 500;
}

.action-create {
  background: #d1fae5;
  color: #065f46;
}

.action-update {
  background: #dbeafe;
  color: #1e40af;
}

.action-delete {
  background: #fee2e2;
  color: #991b1b;
}

.action-login {
  background: #fef3c7;
  color: #92400e;
}

.action-logout {
  background: #e5e7eb;
  color: #374151;
}

.empty-hint {
  text-align: center;
  color: #6b7280;
  padding: 2rem;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 1.5rem;
}

.page-info {
  color: #6b7280;
  font-size: 0.875rem;
}
</style>
