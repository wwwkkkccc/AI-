<template>
  <section class="panel-card">
    <h3>User Governance</h3>
    <div class="toolbar">
      <input :value="adminUserKeyword" type="text" placeholder="Search by username" @input="onKeywordInput" />
      <button class="btn ghost" @click="loadAdminUsers">Refresh</button>
    </div>
    <p class="message">{{ adminUsersMessage }}</p>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Username</th>
            <th>Role</th>
            <th>VIP</th>
            <th>Blacklist</th>
            <th>Created At</th>
            <th>Last Login</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in adminUsers" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td>{{ roleText(user.role) }}</td>
            <td>
              <button class="btn ghost" :disabled="adminUserLoadingId === user.id" @click="toggleVip(user)">
                {{ user.vip ? "Enabled" : "Disabled" }}
              </button>
            </td>
            <td>
              <button class="btn ghost" :disabled="adminUserLoadingId === user.id" @click="toggleBlacklist(user)">
                {{ user.blacklisted ? "Blacklisted" : "Normal" }}
              </button>
            </td>
            <td>{{ formatTime(user.createdAt) }}</td>
            <td>{{ formatTime(user.lastLoginAt) }}</td>
          </tr>
          <tr v-if="!adminUsers.length"><td colspan="7" class="empty">No data</td></tr>
        </tbody>
      </table>
    </div>

    <div class="pager">
      <button class="btn ghost" :disabled="adminUserPage === 0" @click="adminUserGoPage(adminUserPage - 1)">Prev</button>
      <button
        v-for="page in adminUserPageRange"
        :key="`admin-${page}`"
        class="btn ghost"
        :class="{ active: page === adminUserPage }"
        @click="adminUserGoPage(page)"
      >
        {{ page + 1 }}
      </button>
      <button class="btn ghost" :disabled="adminUserPage >= adminUserTotalPages - 1" @click="adminUserGoPage(adminUserPage + 1)">
        Next
      </button>
      <select :value="adminUserSize" @change="adminUserChangeSize($event.target.value)">
        <option :value="10">10</option>
        <option :value="20">20</option>
        <option :value="50">50</option>
      </select>
      <span>Total {{ adminUserTotal }}</span>
    </div>
  </section>
</template>

<script setup>
import { formatTime, roleText } from "../utils/displayFormat";

defineProps({
  adminUserKeyword: { type: String, required: true },
  loadAdminUsers: { type: Function, required: true },
  adminUsersMessage: { type: String, required: true },
  adminUsers: { type: Array, required: true },
  adminUserLoadingId: { type: Number, default: null },
  toggleVip: { type: Function, required: true },
  toggleBlacklist: { type: Function, required: true },
  adminUserPage: { type: Number, required: true },
  adminUserPageRange: { type: Array, required: true },
  adminUserTotalPages: { type: Number, required: true },
  adminUserGoPage: { type: Function, required: true },
  adminUserSize: { type: Number, required: true },
  adminUserChangeSize: { type: Function, required: true },
  adminUserTotal: { type: Number, required: true }
});

const emit = defineEmits(["update:adminUserKeyword"]);

function onKeywordInput(event) {
  emit("update:adminUserKeyword", event?.target?.value ?? "");
}
</script>

