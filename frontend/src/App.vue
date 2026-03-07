<template>
  <div class="app-shell">
    <section v-if="!token" class="auth-layout">
      <div class="auth-card">
        <p class="kicker">Resume AI Workspace</p>
        <h1>{{ authMode === "login" ? "Welcome Back" : "Create Account" }}</h1>
        <p class="subtle">Keep only meaningful capabilities. Focus on one complete resume optimization loop.</p>

        <form class="form-grid" @submit.prevent="submitAuth">
          <label class="field">
            <span>Username</span>
            <input v-model.trim="activeAuthForm.username" type="text" autocomplete="username" required />
          </label>
          <label class="field">
            <span>Password</span>
            <input v-model.trim="activeAuthForm.password" type="password" autocomplete="current-password" required />
          </label>
          <label class="checkbox-row">
            <input v-model="rememberMe" type="checkbox" />
            <span>Remember me</span>
          </label>
          <button class="btn primary" :disabled="authLoading" type="submit">
            {{ authLoading ? "Submitting..." : authMode === "login" ? "Login" : "Register" }}
          </button>
        </form>

        <p class="message">{{ authMessage }}</p>
        <a href="#" class="switch-link" @click.prevent="toggleAuthMode">
          {{ authMode === "login" ? "No account? Register" : "Already have account? Login" }}
        </a>
      </div>
    </section>

    <section v-else class="workspace-layout">
      <aside class="sidebar">
        <div>
          <p class="kicker">Workspace</p>
          <h2>Resume Analysis</h2>
          <p class="subtle">{{ me.username }} · {{ roleText(me.role) }}</p>
          <p class="subtle">{{ me.vip ? "VIP Priority Queue" : "Standard Queue" }}</p>
        </div>

        <nav class="nav-list">
          <button class="nav-item" :class="{ active: tab === 'analyze' }" @click="openTab('analyze')">Analysis Board</button>
          <button class="nav-item" :class="{ active: tab === 'mine' }" @click="openTab('mine')">My Records</button>
          <button v-if="isAdmin" class="nav-item" :class="{ active: tab === 'adminUsers' }" @click="openTab('adminUsers')">
            User Governance
          </button>
        </nav>

        <button class="btn ghost" @click="logout">Logout</button>
      </aside>

      <main class="workspace-main">
        <header class="workspace-header">
          <h1>{{ tabTitle }}</h1>
          <div class="header-tags">
            <span class="tag" v-if="queueJob.jobId">{{ statusText(queueJob.status) }}</span>
            <span class="tag" v-if="result?.analysisId">Record #{{ result.analysisId }}</span>
          </div>
        </header>

        <AnalyzeBoard
          v-if="tab === 'analyze'"
          :analyze-form="analyzeForm"
          :analyze-loading="analyzeLoading"
          :analyze-message="analyzeMessage"
          :queue-job="queueJob"
          :result="result"
          :on-file-change="onFileChange"
          :on-jd-image-change="onJdImageChange"
          :submit-analyze="submitAnalyze"
          :refresh-current-job="refreshCurrentJob"
          :copy-text="copyText"
          :download-text="downloadText"
          :resume-gen-form="resumeGenForm"
          :resume-gen-loading="resumeGenLoading"
          :resume-gen-message="resumeGenMessage"
          :generated-resume="generatedResume"
          :generate-resume-from-jd="generateResumeFromJd"
          :rewrite-resume-from-current-analysis="rewriteResumeFromCurrentAnalysis"
          :copy-generated-resume="copyGeneratedResume"
          :download-generated-resume="downloadGeneratedResume"
          :chat-loading="chatLoading"
          :chat-message="chatMessage"
          :chat-state="chatState"
          :start-resume-chat="startResumeChat"
          :send-chat-message="sendChatMessage"
          :quick-ask="quickAsk"
          :radar-loading="radarLoading"
          :radar-message="radarMessage"
          :jd-radar="jdRadar"
          :run-jd-radar-from-current="runJdRadarFromCurrent"
          :audit-loading="auditLoading"
          :audit-message="auditMessage"
          :resume-audit="resumeAudit"
          :run-audit-from-current="runAuditFromCurrent"
        />
        <MineRecordsPanel
          v-else-if="tab === 'mine'"
          v-model:mine-keyword="mineKeyword"
          :load-mine-analyses="loadMineAnalyses"
          :mine-message="mineMessage"
          :filtered-mine-items="filteredMineItems"
          :mine-page="minePage"
          :mine-page-range="minePageRange"
          :mine-total-pages="mineTotalPages"
          :mine-go-page="mineGoPage"
          :mine-size="mineSize"
          :mine-change-size="mineChangeSize"
          :mine-total="mineTotal"
        />

        <AdminUsersPanel
          v-else-if="tab === 'adminUsers' && isAdmin"
          v-model:admin-user-keyword="adminUserKeyword"
          :load-admin-users="loadAdminUsers"
          :admin-users-message="adminUsersMessage"
          :admin-users="adminUsers"
          :admin-user-loading-id="adminUserLoadingId"
          :toggle-vip="toggleVip"
          :toggle-blacklist="toggleBlacklist"
          :admin-user-page="adminUserPage"
          :admin-user-page-range="adminUserPageRange"
          :admin-user-total-pages="adminUserTotalPages"
          :admin-user-go-page="adminUserGoPage"
          :admin-user-size="adminUserSize"
          :admin-user-change-size="adminUserChangeSize"
          :admin-user-total="adminUserTotal"
        />
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useAnalyzeSubmission } from "./composables/useAnalyzeSubmission";
import { useDataPanels } from "./composables/useDataPanels";
import { useApiClient } from "./composables/useApiClient";
import { useAuthForm } from "./composables/useAuthForm";
import { usePaginationActions } from "./composables/usePaginationActions";
import { useQueuePolling } from "./composables/useQueuePolling";
import { useResumeOperations } from "./composables/useResumeOperations";
import { pageRange, roleText, statusText } from "./utils/displayFormat";
import { toZhMessage } from "./utils/errorMessages";
import { filterMineItemsByKeyword } from "./utils/listFilters";
import { createRequestAbortManager } from "./utils/requestAbort";
import { resetAfterLogout, resetQueueJobSnapshot } from "./utils/sessionState";

const AnalyzeBoard = defineAsyncComponent(() => import("./components/AnalyzeBoard.vue"));
const MineRecordsPanel = defineAsyncComponent(() => import("./components/MineRecordsPanel.vue"));
const AdminUsersPanel = defineAsyncComponent(() => import("./components/AdminUsersPanel.vue"));

const apiBase = "./api";
const tokenKey = "resume_ai_token";

const tab = ref("analyze");
const token = ref(localStorage.getItem(tokenKey) || sessionStorage.getItem(tokenKey) || "");
const me = reactive({ id: null, username: "", role: "", vip: false, blacklisted: false });

// Core analysis workflow state.
const analyzeLoading = ref(false);
const analyzeMessage = ref("");
const analyzeForm = reactive({ targetRole: "", jdText: "", file: null, jdImage: null });
const result = ref(null);
const queueJob = reactive({
  jobId: "",
  status: "",
  queuePosition: null,
  vipPriority: false,
  errorMessage: "",
  createdAt: "",
  startedAt: "",
  finishedAt: ""
});

const resumeGenLoading = ref(false);
const resumeGenMessage = ref("");
const resumeGenForm = reactive({ targetRole: "", jdText: "", userBackground: "" });
const generatedResume = reactive({ mode: "", markdown: "", modelUsed: false, analysisId: null });

const chatLoading = ref(false);
const chatMessage = ref("");
const chatState = reactive({ sessionId: null, messages: [], input: "" });

const radarLoading = ref(false);
const radarMessage = ref("");
const jdRadar = ref(null);

const auditLoading = ref(false);
const auditMessage = ref("");
const resumeAudit = ref(null);

const mineItems = ref([]);
const mineMessage = ref("");
const mineKeyword = ref("");
const minePage = ref(0);
const mineSize = ref(10);
const mineTotal = ref(0);

const adminUsers = ref([]);
const adminUsersMessage = ref("");
const adminUserKeyword = ref("");
const adminUserLoadingId = ref(null);
const adminUserPage = ref(0);
const adminUserSize = ref(10);
const adminUserTotal = ref(0);

const requestAbort = createRequestAbortManager();
const { apiRequest } = useApiClient({ apiBase, token });
const isAdmin = computed(() => String(me.role || "").toUpperCase() === "ADMIN");
const tabTitle = computed(() => (tab.value === "mine" ? "My Analysis Records" : tab.value === "adminUsers" ? "User Governance" : "Resume Analysis Board"));
const filteredMineItems = computed(() => filterMineItemsByKeyword(mineItems.value, mineKeyword.value));

const { loadMineAnalyses, loadAdminUsers, toggleVip, toggleBlacklist } = useDataPanels({
  token,
  isAdmin,
  minePage,
  mineSize,
  mineItems,
  mineTotal,
  mineMessage,
  adminUserKeyword,
  adminUserPage,
  adminUserSize,
  adminUsers,
  adminUserTotal,
  adminUsersMessage,
  adminUserLoadingId,
  requestAbort,
  apiRequest,
  toZhMessage
});

const { totalPages: mineTotalPages, goPage: mineGoPage, changeSize: mineChangeSize } = usePaginationActions({
  page: minePage,
  size: mineSize,
  total: mineTotal,
  load: loadMineAnalyses
});
const { totalPages: adminUserTotalPages, goPage: adminUserGoPage, changeSize: adminUserChangeSize } = usePaginationActions({
  page: adminUserPage,
  size: adminUserSize,
  total: adminUserTotal,
  load: loadAdminUsers
});

const minePageRange = computed(() => pageRange(minePage.value, mineTotalPages.value));
const adminUserPageRange = computed(() => pageRange(adminUserPage.value, adminUserTotalPages.value));

const { stopPolling, startPolling, refreshCurrentJob } = useQueuePolling({
  apiRequest,
  queueJob,
  analyzeMessage,
  result,
  loadMineAnalyses,
  toZhMessage
});

const { onFileChange, onJdImageChange, submitAnalyze } = useAnalyzeSubmission({
  analyzeForm,
  analyzeLoading,
  analyzeMessage,
  result,
  chatState,
  chatMessage,
  jdRadar,
  radarMessage,
  resumeAudit,
  auditMessage,
  stopPolling,
  queueJob,
  resetQueueJobSnapshot,
  apiRequest,
  startPolling,
  toZhMessage
});

const { authLoading, authMessage, authMode, rememberMe, activeAuthForm, toggleAuthMode, submitAuth } = useAuthForm({
  token,
  tokenKey,
  apiRequest,
  loadMe,
  onLoggedIn: async () => {
    tab.value = "analyze";
    await loadMineAnalyses();
    if (isAdmin.value) await loadAdminUsers();
  }
});

async function copyText(text) {
  const content = String(text || "").trim();
  if (!content) return;
  try {
    await navigator.clipboard.writeText(content);
  } catch {
    // ignore clipboard failure
  }
}

function downloadText(filename, text) {
  const content = String(text || "");
  if (!content) return;
  const blob = new Blob([content], { type: "text/markdown;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

async function loadMe() {
  if (!token.value) return;
  const data = await apiRequest("/auth/me");
  me.id = data.id;
  me.username = data.username;
  me.role = data.role;
  me.vip = !!data.vip;
  me.blacklisted = !!data.blacklisted;
}

async function logout() {
  try {
    await apiRequest("/auth/logout", { method: "POST" });
  } catch {
    // ignore
  }

  stopPolling();
  requestAbort.cancelAll();

  // Clear all user-bound runtime data to avoid cross-account residue.
  resetAfterLogout({
    tokenKey,
    token,
    me,
    tab,
    queueJob,
    result,
    analyzeMessage,
    analyzeForm,
    resumeGenLoading,
    resumeGenMessage,
    resumeGenForm,
    generatedResume,
    chatLoading,
    chatMessage,
    chatState,
    radarLoading,
    radarMessage,
    jdRadar,
    auditLoading,
    auditMessage,
    resumeAudit,
    mineItems,
    mineMessage,
    mineKeyword,
    minePage,
    mineTotal,
    adminUsers,
    adminUsersMessage,
    adminUserKeyword,
    adminUserPage,
    adminUserTotal
  });
}

async function openTab(nextTab) {
  if (nextTab === "adminUsers" && !isAdmin.value) return;
  tab.value = nextTab;
  if (nextTab === "mine") await loadMineAnalyses();
  if (nextTab === "adminUsers") await loadAdminUsers();
}

const {
  generateResumeFromJd,
  rewriteResumeFromCurrentAnalysis,
  copyGeneratedResume,
  downloadGeneratedResume,
  startResumeChat,
  sendChatMessage,
  quickAsk,
  runJdRadarFromCurrent,
  runAuditFromCurrent
} = useResumeOperations({
  result,
  resumeGenForm,
  resumeGenLoading,
  resumeGenMessage,
  generatedResume,
  chatLoading,
  chatMessage,
  chatState,
  radarLoading,
  radarMessage,
  jdRadar,
  auditLoading,
  auditMessage,
  resumeAudit,
  analyzeForm,
  apiRequest,
  toZhMessage,
  copyText,
  downloadText
});

onMounted(async () => {
  if (!token.value) return;
  try {
    await loadMe();
    await loadMineAnalyses();
    if (isAdmin.value) await loadAdminUsers();
  } catch (err) {
    await logout();
    authMessage.value = toZhMessage(err?.message || "login state expired");
  }
});

onBeforeUnmount(() => {
  stopPolling();
  requestAbort.cancelAll();
});
</script>
