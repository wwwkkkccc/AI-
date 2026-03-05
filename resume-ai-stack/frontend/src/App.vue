<template>
  <div class="page" :class="{ 'page-auth': !token }">
    <header v-if="token" class="top">
      <h1>AI 简历分析系统</h1>
      <p>支持排队分析、VIP 优先、用户管理与配置管理</p>
    </header>

    <section v-if="!token" class="auth-screen">
      <div class="auth-wrapper">
        <form class="auth-form" @submit.prevent="submitAuth">
          <h2>{{ authMode === 'login' ? '登录' : '注册' }}</h2>

          <div class="input-field">
            <input v-model.trim="activeAuthForm.username" type="text" required placeholder=" " />
            <label>请输入用户名</label>
          </div>

          <div class="input-field">
            <input v-model.trim="activeAuthForm.password" type="password" required placeholder=" " />
            <label>请输入密码</label>
          </div>

          <div class="forget">
            <label for="remember">
              <input id="remember" v-model="rememberMe" type="checkbox" />
              <p>记住我</p>
            </label>
            <a href="#" @click.prevent="toggleAuthMode">
              {{ authMode === 'login' ? '去注册' : '去登录' }}
            </a>
          </div>

          <button :disabled="authLoading" type="submit">
            {{ authLoading ? '提交中...' : authMode === 'login' ? '登录' : '注册' }}
          </button>

          <div class="register">
            <p v-if="authMode === 'login'">
              还没有账号？<a href="#" @click.prevent="setAuthMode('register')">立即注册</a>
            </p>
            <p v-else>
              已有账号？<a href="#" @click.prevent="setAuthMode('login')">立即登录</a>
            </p>
          </div>
          <p class="auth-message">{{ authMessage }}</p>
        </form>
      </div>
    </section>

    <template v-else>
      <div class="workspace">
        <aside class="side-panel">
          <div class="brand-block">
            <h1>AI 简历分析系统</h1>
            <p>淡雅灰与暗金色工作台</p>
          </div>

          <section class="side-user">
            <div>
              <strong>{{ me.username }}</strong>
              <span class="pill">{{ roleText(me.role) }}</span>
              <span v-if="me.vip" class="pill vip-pill">VIP</span>
              <span v-if="me.blacklisted" class="pill danger-pill">已拉黑</span>
            </div>
          </section>

          <section class="tabs side-tabs">
            <button :class="{ active: tab === 'analyze' }" @click="tab = 'analyze'">简历分析</button>
            <button :class="{ active: tab === 'mine' }" @click="switchMine">我的记录</button>
            <button v-if="isAdmin" :class="{ active: tab === 'adminUsers' }" @click="switchAdminUsers">用户管理</button>
            <button v-if="isAdmin" :class="{ active: tab === 'adminConfig' }" @click="switchAdminConfig">模型配置</button>
            <button v-if="isAdmin" :class="{ active: tab === 'adminData' }" @click="switchAdminData">客户简历</button>
            <button v-if="isAdmin" :class="{ active: tab === 'adminKb' }" @click="switchAdminKb">面试题库</button>
          </section>

          <p class="side-note">VIP 用户任务会被优先消费</p>
        </aside>

        <main class="content-panel">
          <section class="main-head">
            <div>
              <h2>{{ tabTitle(tab) }}</h2>
              <p>支持排队分析、VIP 优先、用户管理与配置管理</p>
            </div>
            <button class="ghost" @click="logout">退出登录</button>
          </section>

          <section v-if="tab === 'analyze'" class="card">
        <h2>简历分析</h2>
        <form @submit.prevent="submitAnalyze">
          <label>目标岗位</label>
          <input v-model.trim="analyzeForm.targetRole" type="text" placeholder="例如：后端开发工程师" />

          <label>简历文件（pdf/docx/txt）</label>
          <input type="file" accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg,.bmp,.webp,.tif,.tiff" @change="onFileChange" required />

          <label>岗位描述（JD，可直接粘贴）</label>
          <textarea v-model.trim="analyzeForm.jdText" rows="8" placeholder="可粘贴JD文本；如果无法复制，可上传JD截图" />

          <label>岗位JD图片（可选，支持 png/jpg/jpeg/webp）</label>
          <input type="file" accept=".png,.jpg,.jpeg,.bmp,.webp,.tif,.tiff" @change="onJdImageChange" />

          <button :disabled="analyzeLoading">{{ analyzeLoading ? '提交中...' : '提交分析任务' }}</button>
        </form>
        <p class="message">{{ analyzeMessage }}</p>

        <div v-if="queueJob.jobId" class="queue-panel">
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
              <strong>{{ queueJob.queuePosition ?? '-' }}</strong>
            </div>
            <div>
              <span>优先级</span>
              <strong>{{ queueJob.vipPriority ? 'VIP优先' : '普通队列' }}</strong>
            </div>
          </div>
          <button class="mini-btn" @click="refreshCurrentJob" :disabled="!queueJob.jobId">手动刷新状态</button>
        </div>

        <div v-if="result" class="result">
          <div class="metrics">
            <div><span>匹配评分</span><strong>{{ result.score }}</strong></div>
            <div><span>关键词覆盖率</span><strong>{{ result.coverage }}%</strong></div>
          </div>
          <h3>已匹配关键词</h3>
          <p>{{ (result.matchedKeywords || []).join(', ') || '-' }}</p>
          <h3>缺失关键词</h3>
          <p>{{ (result.missingKeywords || []).join(', ') || '-' }}</p>
          <h3>优化摘要</h3>
          <p>{{ result.optimized?.summary || '-' }}</p>
          <h3>可能面试问题</h3>
          <ul>
            <li v-for="(q, idx) in result.optimized?.interviewQuestions || []" :key="idx">{{ q }}</li>
          </ul>
        </div>
      </section>

      <section v-else-if="tab === 'mine'" class="card">
        <h2>我的简历记录</h2>
        <div class="record-toolbar">
          <p class="message">{{ mineMessage }}</p>
          <span class="record-tip">按时间倒序展示，重点建议可展开查看</span>
        </div>
        <div class="table-wrap">
          <table class="lux-table mine-table">
            <thead>
              <tr>
                <th>编号</th>
                <th>文件</th>
                <th>岗位</th>
                <th>评分</th>
                <th>覆盖率</th>
                <th>建议</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in mineItems" :key="item.id">
                <td class="cell-id">#{{ item.id }}</td>
                <td class="file-cell">
                  <div class="file-name">{{ item.filename || '-' }}</div>
                </td>
                <td>
                  <span class="role-tag">{{ item.targetRole || '-' }}</span>
                </td>
                <td>
                  <span class="score-pill" :class="scoreLevelClass(item.score)">{{ formatScore(item.score) }}</span>
                </td>
                <td>
                  <div class="coverage-wrap">
                    <div class="coverage-track">
                      <span :style="{ width: coveragePercent(item.coverage) }"></span>
                    </div>
                    <span class="coverage-text">{{ formatCoverage(item.coverage) }}</span>
                  </div>
                </td>
                <td class="summary-cell">
                  <p class="summary-clamp">{{ item.optimizedSummary || '-' }}</p>
                  <details v-if="(item.optimizedSummary || '').length > 120" class="detail inline-detail">
                    <summary>展开完整建议</summary>
                    <pre>{{ item.optimizedSummary }}</pre>
                  </details>
                </td>
                <td class="time-cell">{{ formatTime(item.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-else-if="tab === 'adminUsers'" class="card">
        <h2>管理员 - 用户管理</h2>
        <div class="inline">
          <input v-model.trim="adminUserQuery.keyword" type="text" placeholder="按用户名搜索" />
          <button @click="loadAdminUsers">搜索</button>
        </div>
        <p class="message">{{ adminUsersMessage }}</p>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>用户名</th>
                <th>角色</th>
                <th>VIP</th>
                <th>拉黑</th>
                <th>创建时间</th>
                <th>最近登录</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in adminUsers" :key="item.id">
                <td>{{ item.id }}</td>
                <td>{{ item.username }}</td>
                <td>{{ roleText(item.role) }}</td>
                <td>{{ booleanText(item.vip) }}</td>
                <td>{{ booleanText(item.blacklisted) }}</td>
                <td>{{ formatTime(item.createdAt) }}</td>
                <td>{{ formatTime(item.lastLoginAt) }}</td>
                <td>
                  <div class="row-actions">
                    <button
                      class="mini-btn"
                      :disabled="adminUserLoadingId === item.id"
                      @click="toggleVip(item)"
                    >
                      {{ item.vip ? '取消VIP' : '设为VIP' }}
                    </button>
                    <button
                      class="mini-btn warn"
                      :disabled="adminUserLoadingId === item.id || (item.role || '').toUpperCase() === 'ADMIN'"
                      @click="toggleBlacklist(item)"
                    >
                      {{ item.blacklisted ? '取消拉黑' : '拉黑用户' }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-else-if="tab === 'adminConfig'" class="card">
        <h2>管理员 - 模型配置</h2>
        <form @submit.prevent="saveConfig">
          <label>接口地址（Base URL）</label>
          <input v-model.trim="configForm.baseUrl" type="text" placeholder="https://aicodelink.shop" />

          <label>API Key</label>
          <input v-model.trim="configForm.apiKey" type="text" placeholder="sk-..." />

          <label>模型名称</label>
          <input v-model.trim="configForm.model" type="text" placeholder="gpt-5.3-codex" />

          <button :disabled="configLoading">{{ configLoading ? '保存中...' : '保存配置' }}</button>
        </form>
        <p class="message">{{ configMessage }}</p>
        <p class="meta" v-if="configUpdatedAt">最近更新时间：{{ formatTime(configUpdatedAt) }}</p>
      </section>

      <section v-else-if="tab === 'adminData'" class="card">
        <h2>管理员 - 客户简历数据</h2>
        <div class="inline">
          <input v-model.trim="adminQuery.username" type="text" placeholder="按用户名搜索" />
          <button @click="loadAdminAnalyses">搜索</button>
        </div>
        <div class="record-toolbar">
          <p class="message">{{ adminMessage }}</p>
          <span class="record-tip">支持预览摘要，完整内容按需展开</span>
        </div>
        <div class="table-wrap">
          <table class="lux-table admin-data-table">
            <thead>
              <tr>
                <th>用户</th>
                <th>文件</th>
                <th>岗位</th>
                <th>评分</th>
                <th>覆盖率</th>
                <th>模型分析</th>
                <th>简历预览</th>
                <th>建议</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in adminItems" :key="item.id">
                <td>
                  <div class="user-badge">{{ item.username || '-' }}</div>
                </td>
                <td class="file-cell">
                  <div class="file-name">{{ item.filename || '-' }}</div>
                </td>
                <td>
                  <span class="role-tag">{{ item.targetRole || '-' }}</span>
                </td>
                <td>
                  <span class="score-pill" :class="scoreLevelClass(item.score)">{{ formatScore(item.score) }}</span>
                </td>
                <td>
                  <div class="coverage-wrap">
                    <div class="coverage-track">
                      <span :style="{ width: coveragePercent(item.coverage) }"></span>
                    </div>
                    <span class="coverage-text">{{ formatCoverage(item.coverage) }}</span>
                  </div>
                </td>
                <td>
                  <span class="state-pill" :class="item.modelUsed ? 'state-yes' : 'state-no'">
                    {{ item.modelUsed ? '已启用' : '未启用' }}
                  </span>
                </td>
                <td class="summary-cell">
                  <p class="summary-clamp">{{ item.resumePreview || '-' }}</p>
                  <details class="detail inline-detail">
                    <summary>完整简历</summary>
                    <pre>{{ item.resumeText || '-' }}</pre>
                  </details>
                  <details class="detail inline-detail">
                    <summary>岗位JD</summary>
                    <pre>{{ item.jdText || '-' }}</pre>
                  </details>
                </td>
                <td class="summary-cell">
                  <p class="summary-clamp">{{ item.optimizedSummary || '-' }}</p>
                  <details v-if="(item.optimizedSummary || '').length > 120" class="detail inline-detail">
                    <summary>展开完整建议</summary>
                    <pre>{{ item.optimizedSummary }}</pre>
                  </details>
                </td>
                <td class="time-cell">{{ formatTime(item.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-else class="card">
        <h2>管理员 - 面试题库</h2>
        <div class="kb-grid">
          <form class="kb-block" @submit.prevent="uploadKbDoc">
            <h3>文档上传入库</h3>
            <label>文档标题（可选）</label>
            <input v-model.trim="kbUploadForm.title" type="text" placeholder="例如：Java后端高频面试题（2026）" />

            <label>上传面试文档（pdf/docx/txt/md/png/jpg）</label>
            <input type="file" accept=".pdf,.docx,.txt,.md,.png,.jpg,.jpeg,.bmp,.webp,.tif,.tiff" @change="onKbFileChange" required />

            <button :disabled="kbUploadLoading || kbCrawlLoading || kbLlmLoading">{{ kbUploadLoading ? '上传中...' : '上传并入库' }}</button>
          </form>

          <form class="kb-block" @submit.prevent="crawlKbDocs">
            <h3>爬虫一键入库</h3>
            <label>种子链接（必填）</label>
            <input v-model.trim="kbCrawlForm.seedUrl" type="url" placeholder="https://example.com/interview-guide" required />

            <label>文档标题（可选）</label>
            <input v-model.trim="kbCrawlForm.title" type="text" placeholder="例如：互联网行业面试知识库" />

            <label>主题标签（可选）</label>
            <input v-model.trim="kbCrawlForm.topic" type="text" placeholder="例如：后端 / 产品 / 运营 / 金融" />

            <div class="inline">
              <div class="field-mini">
                <label>最大页数</label>
                <input v-model.number="kbCrawlForm.maxPages" type="number" min="1" max="80" />
              </div>
              <div class="field-mini">
                <label>最大深度</label>
                <input v-model.number="kbCrawlForm.maxDepth" type="number" min="0" max="3" />
              </div>
            </div>

            <label class="checkbox-line">
              <input v-model="kbCrawlForm.sameDomainOnly" type="checkbox" />
              仅抓取同域链接
            </label>

            <label>私有站点 Cookie（可选）</label>
            <textarea
              v-model.trim="kbCrawlForm.authCookie"
              rows="3"
              placeholder="例如：sessionid=xxx; csrftoken=yyy"
            />

            <label>Referer（可选）</label>
            <input
              v-model.trim="kbCrawlForm.referer"
              type="text"
              placeholder="例如：https://xxx.com/login"
            />

            <label>Authorization Header（可选）</label>
            <input
              v-model.trim="kbCrawlForm.authHeader"
              type="text"
              placeholder="例如：Bearer eyJ..."
            />

            <button :disabled="kbUploadLoading || kbCrawlLoading || kbLlmLoading">{{ kbCrawlLoading ? '爬取中...' : '开始爬取并入库' }}</button>
          </form>

          <form class="kb-block" @submit.prevent="llmImportKbDocs">
            <h3>大模型生成入库</h3>
            <label>知识主题（必填）</label>
            <input v-model.trim="kbLlmForm.topic" type="text" placeholder="例如：Java 并发、MySQL 调优、产品设计" required />

            <label>接口 URL（必填）</label>
            <input v-model.trim="kbLlmForm.baseUrl" type="url" placeholder="https://aicodelink.shop 或 https://api.openai.com/v1" required />

            <label>API Key（必填）</label>
            <input v-model.trim="kbLlmForm.apiKey" type="password" placeholder="sk-..." required />

            <label>模型名称（可选）</label>
            <input v-model.trim="kbLlmForm.model" type="text" placeholder="gpt-5.3-codex" />

            <div class="inline">
              <div class="field-mini">
                <label>题目数量</label>
                <input v-model.number="kbLlmForm.questionCount" type="number" min="5" max="120" />
              </div>
              <div class="field-mini">
                <label>文档标题</label>
                <input v-model.trim="kbLlmForm.title" type="text" placeholder="可选" />
              </div>
            </div>

            <button :disabled="kbUploadLoading || kbCrawlLoading || kbLlmLoading">{{ kbLlmLoading ? '生成中...' : '生成并入库' }}</button>
          </form>
        </div>
        <p class="message">{{ kbMessage }}</p>
        <details v-if="kbCrawlErrors.length" class="detail">
          <summary>本次爬取失败页面（{{ kbCrawlErrors.length }}）</summary>
          <pre>{{ kbCrawlErrors.join('\n') }}</pre>
        </details>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>标题</th>
                <th>文件名</th>
                <th>题目数</th>
                <th>上传人</th>
                <th>时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in kbDocs" :key="item.id">
                <td>{{ item.id }}</td>
                <td>{{ item.title || '-' }}</td>
                <td>{{ item.filename || '-' }}</td>
                <td>{{ item.questionCount ?? 0 }}</td>
                <td>{{ item.uploadedBy || '-' }}</td>
                <td>{{ formatTime(item.createdAt) }}</td>
                <td>
                  <div class="row-actions">
                    <button class="mini-btn neutral" @click="openKbDocViewer(item)" :disabled="kbUploadLoading || kbCrawlLoading || kbLlmLoading || kbViewLoading">查看</button>
                    <button class="mini-btn warn" @click="deleteKbDoc(item.id)" :disabled="kbUploadLoading || kbCrawlLoading || kbLlmLoading || kbViewLoading">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <section v-if="kbViewer.visible" class="kb-viewer">
          <div class="viewer-head">
            <div>
              <h3>文件内容预览</h3>
              <p>{{ kbViewer.title || "-" }}（{{ kbViewer.filename || "-" }}）</p>
            </div>
            <button class="mini-btn neutral" @click="closeKbDocViewer" :disabled="kbViewLoading">关闭</button>
          </div>
          <p class="message">{{ kbViewMessage }}</p>
          <ol class="viewer-list">
            <li v-for="(q, idx) in kbViewer.questions" :key="`${kbViewer.docId}-${idx}`">{{ q }}</li>
          </ol>
          <div class="viewer-actions">
            <button v-if="kbViewerHasMore" class="mini-btn" @click="loadKbDocViewerMore" :disabled="kbViewLoading">
              {{ kbViewLoading ? "加载中..." : "加载更多" }}
            </button>
          </div>
        </section>
      </section>
        </main>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";

const apiBase = "./api";
const tokenKey = "resume_ai_token";

const tab = ref("analyze");
const token = ref(localStorage.getItem(tokenKey) || sessionStorage.getItem(tokenKey) || "");
const me = reactive({ id: null, username: "", role: "", vip: false, blacklisted: false });
const authLoading = ref(false);
const authMessage = ref("");
const authMode = ref("login");
const rememberMe = ref(true);

const loginForm = reactive({ username: "", password: "" });
const registerForm = reactive({ username: "", password: "" });

const analyzeLoading = ref(false);
const analyzeMessage = ref("");
const result = ref(null);
const analyzeForm = reactive({
  targetRole: "",
  jdText: "",
  file: null,
  jdImage: null
});

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

let pollTimer = null;

const mineItems = ref([]);
const mineMessage = ref("");

const adminUsers = ref([]);
const adminUserQuery = reactive({ keyword: "" });
const adminUsersMessage = ref("");
const adminUserLoadingId = ref(null);

const configLoading = ref(false);
const configMessage = ref("");
const configUpdatedAt = ref("");
const configForm = reactive({
  baseUrl: "",
  apiKey: "",
  model: ""
});

const adminQuery = reactive({ username: "" });
const adminItems = ref([]);
const adminMessage = ref("");
const kbDocs = ref([]);
const kbMessage = ref("");
const kbUploadLoading = ref(false);
const kbCrawlLoading = ref(false);
const kbLlmLoading = ref(false);
const kbCrawlErrors = ref([]);
const kbUploadForm = reactive({
  title: "",
  file: null
});
const kbCrawlForm = reactive({
  seedUrl: "",
  title: "",
  topic: "",
  maxPages: 20,
  maxDepth: 1,
  sameDomainOnly: true,
  authCookie: "",
  referer: "",
  authHeader: ""
});
const kbLlmForm = reactive({
  topic: "",
  title: "",
  baseUrl: "",
  apiKey: "",
  model: "",
  questionCount: 30
});
const kbViewLoading = ref(false);
const kbViewMessage = ref("");
const kbViewer = reactive({
  visible: false,
  docId: null,
  title: "",
  filename: "",
  total: 0,
  page: 0,
  size: 100,
  questions: []
});

const isAdmin = computed(() => (me.role || "").toUpperCase() === "ADMIN");
const activeAuthForm = computed(() => (authMode.value === "login" ? loginForm : registerForm));
const kbViewerHasMore = computed(() => kbViewer.questions.length < kbViewer.total);

function authHeaders(extra = {}) {
  const headers = { ...extra };
  if (token.value) {
    headers.Authorization = `Bearer ${token.value}`;
  }
  return headers;
}

function toZhMessage(message) {
  const msg = String(message || "").trim();
  const map = {
    "invalid username or password": "用户名或密码错误",
    "missing auth token": "请先登录",
    "token expired or invalid": "登录已过期，请重新登录",
    "admin access required": "需要管理员权限",
    "registration limit reached for this IP today": "当前IP今日注册次数已达上限（3次）",
    "username already exists": "用户名已存在",
    "username format invalid, use 4-32 letters/numbers/_": "用户名格式错误（4-32位字母/数字/下划线）",
    "password must be at least 8 characters": "密码至少8位",
    "request body must be valid json": "请求格式错误，请刷新页面后重试",
    "request validation failed": "请求参数校验失败，请检查输入",
    "unsupported content type, use application/json or application/x-www-form-urlencoded": "请求类型不支持，请使用 JSON 或表单格式提交",
    "internal server error": "服务器内部错误",
    "account is blacklisted": "账号已被拉黑",
    "cannot blacklist admin": "管理员账号不能被拉黑",
    "user not found": "用户不存在",
    "file is required": "请上传简历文件",
    "file is too large": "文件过大，请上传5MB以内文件",
    "jd_text should have at least 20 characters": "岗位描述至少需要20个字符（或填写目标岗位）",
    "failed to save upload file": "上传文件保存失败",
    "failed to read queued resume file": "排队任务读取简历失败",
    "jd image is too large": "JD 图片过大，请上传5MB以内图片",
    "jd image format not supported": "JD 图片格式不支持，请上传 png/jpg/jpeg/webp/bmp/tif",
    "jd image text is too short": "JD 图片识别出的文字太少，请换更清晰的截图",
    "failed to read jd image": "JD 图片识别失败，请换清晰图片或直接粘贴文字",
    "resume text is too short": "简历内容过短，请检查文件",
    "failed to read resume file": "简历文件读取失败",
    "job not found": "任务不存在",
    "cannot access this job": "无权访问该任务",
    "question doc file is required": "请先选择面试文档",
    "question doc file is too large": "面试文档过大，请上传12MB以内文件",
    "question doc format not supported": "文档格式不支持，请上传 pdf/docx/txt/md/图片",
    "failed to read question doc file": "面试文档解析失败，请换一个文件",
    "no interview questions found in document": "文档中未识别到面试问题，请检查内容",
    "question doc not found": "题库文档不存在或已删除",
    "seed_url is required": "请填写爬虫种子链接",
    "seed_url is invalid": "爬虫链接格式不正确",
    "seed_url must start with http or https": "爬虫链接必须以 http:// 或 https:// 开头",
    "seed_url host is not allowed": "该链接域名不允许抓取",
    "max_pages must be between 1 and 80": "最大页数范围为 1-80",
    "max_depth must be between 0 and 3": "最大深度范围为 0-3",
    "private page requires login cookie": "该页面是私有内容，请在爬虫表单填写有效 Cookie 或 Authorization",
    "no interview questions found in crawled pages": "爬取页面中未识别到可用面试题",
    "llm_topic is required": "请填写知识主题",
    "llm_base_url is required": "请填写模型接口 URL",
    "llm_api_key is required": "请填写模型 API Key",
    "question_count must be between 5 and 120": "题目数量范围为 5-120",
    "no interview questions generated by model": "模型未返回可用面试题，请换主题或模型",
    "llm request failed": "调用大模型失败，请检查 URL、API Key 和模型",
    "llm response is empty": "模型返回为空，请重试",
    queued: "已进入队列",
    "VIP priority queued": "已进入VIP优先队列"
  };
  return map[msg] || msg || "请求失败";
}

function roleText(role) {
  return (role || "").toUpperCase() === "ADMIN" ? "管理员" : "普通用户";
}

function booleanText(v) {
  return v ? "是" : "否";
}

function asNumber(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

function formatScore(value) {
  return asNumber(value).toFixed(2);
}

function scoreLevelClass(value) {
  const n = asNumber(value);
  if (n >= 75) return "score-high";
  if (n >= 55) return "score-mid";
  return "score-low";
}

function coveragePercent(value) {
  const n = Math.max(0, Math.min(100, asNumber(value)));
  return `${n}%`;
}

function formatCoverage(value) {
  const n = Math.max(0, Math.min(100, asNumber(value)));
  const rounded = Math.round(n * 100) / 100;
  return Number.isInteger(rounded) ? `${rounded}%` : `${rounded.toFixed(2)}%`;
}

function statusText(status) {
  const map = {
    PENDING: "排队中",
    PROCESSING: "分析中",
    DONE: "已完成",
    FAILED: "失败"
  };
  return map[status] || status || "-";
}

function statusClass(status) {
  const v = String(status || "").toUpperCase();
  if (v === "DONE") return "status-done";
  if (v === "FAILED") return "status-failed";
  if (v === "PROCESSING") return "status-processing";
  return "status-pending";
}

function tabTitle(tabKey) {
  const map = {
    analyze: "简历分析",
    mine: "我的记录",
    adminUsers: "用户管理",
    adminConfig: "模型配置",
    adminData: "客户简历",
    adminKb: "面试题库"
  };
  return map[tabKey] || "工作台";
}

async function apiRequest(path, options = {}) {
  const opts = { ...options, headers: authHeaders(options.headers || {}) };
  const res = await fetch(`${apiBase}${path}`, opts);
  let data = {};
  try {
    data = await res.json();
  } catch {
    data = {};
  }
  if (!res.ok) {
    throw new Error(toZhMessage(data?.detail || data?.message || `请求失败: ${res.status}`));
  }
  return data;
}

function persistToken(newToken) {
  token.value = newToken;
  sessionStorage.removeItem(tokenKey);
  localStorage.removeItem(tokenKey);
  if (rememberMe.value) {
    localStorage.setItem(tokenKey, newToken);
  } else {
    sessionStorage.setItem(tokenKey, newToken);
  }
}

function setAuthMode(mode) {
  authMode.value = mode;
  authMessage.value = "";
}

function toggleAuthMode() {
  setAuthMode(authMode.value === "login" ? "register" : "login");
}

async function submitLogin() {
  authLoading.value = true;
  authMessage.value = "";
  try {
    const data = await apiRequest("/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(loginForm)
    });
    persistToken(data.token);
    await loadMe();
    authMessage.value = "登录成功";
    tab.value = "analyze";
  } catch (err) {
    authMessage.value = toZhMessage(err.message || "登录失败");
  } finally {
    authLoading.value = false;
  }
}

async function submitRegister() {
  authLoading.value = true;
  authMessage.value = "";
  try {
    const data = await apiRequest("/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(registerForm)
    });
    persistToken(data.token);
    await loadMe();
    authMessage.value = "注册成功";
    tab.value = "analyze";
  } catch (err) {
    authMessage.value = toZhMessage(err.message || "注册失败");
  } finally {
    authLoading.value = false;
  }
}

async function submitAuth() {
  if (authMode.value === "login") {
    await submitLogin();
  } else {
    await submitRegister();
  }
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
}

function resetQueueJob() {
  queueJob.jobId = "";
  queueJob.status = "";
  queueJob.queuePosition = null;
  queueJob.vipPriority = false;
  queueJob.errorMessage = "";
  queueJob.createdAt = "";
  queueJob.startedAt = "";
  queueJob.finishedAt = "";
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
  resetQueueJob();
  token.value = "";
  localStorage.removeItem(tokenKey);
  sessionStorage.removeItem(tokenKey);
  me.id = null;
  me.username = "";
  me.role = "";
  me.vip = false;
  me.blacklisted = false;
  result.value = null;
  mineItems.value = [];
  adminUsers.value = [];
  adminItems.value = [];
  kbDocs.value = [];
  kbUploadForm.title = "";
  kbUploadForm.file = null;
  kbCrawlForm.seedUrl = "";
  kbCrawlForm.title = "";
  kbCrawlForm.topic = "";
  kbCrawlForm.maxPages = 20;
  kbCrawlForm.maxDepth = 1;
  kbCrawlForm.sameDomainOnly = true;
  kbCrawlForm.authCookie = "";
  kbCrawlForm.referer = "";
  kbCrawlForm.authHeader = "";
  kbLlmForm.topic = "";
  kbLlmForm.title = "";
  kbLlmForm.baseUrl = "";
  kbLlmForm.apiKey = "";
  kbLlmForm.model = "";
  kbLlmForm.questionCount = 30;
  kbCrawlErrors.value = [];
  closeKbDocViewer();
}

function onFileChange(e) {
  const files = e.target.files || [];
  analyzeForm.file = files[0] || null;
}

function onJdImageChange(e) {
  const files = e.target.files || [];
  analyzeForm.jdImage = files[0] || null;
}

function onKbFileChange(e) {
  const files = e.target.files || [];
  kbUploadForm.file = files[0] || null;
}

async function pollJobStatus(jobId) {
  if (!jobId) return;
  try {
    const statusData = await apiRequest(`/analyze/jobs/${jobId}`);
    queueJob.jobId = statusData.jobId || jobId;
    queueJob.status = statusData.status || "";
    queueJob.queuePosition = statusData.queuePosition ?? null;
    queueJob.errorMessage = statusData.errorMessage || "";
    queueJob.createdAt = statusData.createdAt || "";
    queueJob.startedAt = statusData.startedAt || "";
    queueJob.finishedAt = statusData.finishedAt || "";

    if (queueJob.status === "PENDING") {
      const posText = queueJob.queuePosition ? `，当前排队位置：${queueJob.queuePosition}` : "";
      analyzeMessage.value = `任务已提交，排队中${posText}`;
      return;
    }

    if (queueJob.status === "PROCESSING") {
      analyzeMessage.value = "任务正在分析中，请稍候...";
      return;
    }

    if (queueJob.status === "DONE") {
      stopPolling();
      result.value = statusData.result || null;
      analyzeMessage.value = `分析完成，记录ID：${statusData.result?.analysisId || '-'} `;
      await loadMineAnalyses();
      return;
    }

    if (queueJob.status === "FAILED") {
      stopPolling();
      analyzeMessage.value = `分析失败：${toZhMessage(queueJob.errorMessage || "未知错误")}`;
    }
  } catch (err) {
    stopPolling();
    analyzeMessage.value = toZhMessage(err.message || "获取任务状态失败");
  }
}

function startPolling(jobId) {
  stopPolling();
  pollJobStatus(jobId);
  pollTimer = setInterval(() => {
    pollJobStatus(jobId);
  }, 1500);
}

async function refreshCurrentJob() {
  if (!queueJob.jobId) return;
  await pollJobStatus(queueJob.jobId);
}

async function submitAnalyze() {
  if (!analyzeForm.file) {
    analyzeMessage.value = "请先选择简历文件";
    return;
  }
  analyzeLoading.value = true;
  analyzeMessage.value = "";
  result.value = null;
  stopPolling();
  resetQueueJob();

  try {
    const fd = new FormData();
    fd.append("file", analyzeForm.file);
    fd.append("jd_text", analyzeForm.jdText || "");
    fd.append("target_role", analyzeForm.targetRole || "");
    if (analyzeForm.jdImage) {
      fd.append("jd_image", analyzeForm.jdImage);
    }

    const res = await fetch(`${apiBase}/analyze`, {
      method: "POST",
      headers: authHeaders(),
      body: fd
    });

    let data = {};
    try {
      data = await res.json();
    } catch {
      data = {};
    }

    if (!res.ok) {
      throw new Error(toZhMessage(data?.detail || `提交任务失败: ${res.status}`));
    }

    queueJob.jobId = data.jobId;
    queueJob.status = data.status || "PENDING";
    queueJob.queuePosition = data.queuePosition ?? null;
    queueJob.vipPriority = !!data.vipPriority;
    analyzeMessage.value = `${toZhMessage(data.message || "queued")}，任务ID：${data.jobId}`;

    startPolling(data.jobId);
  } catch (err) {
    analyzeMessage.value = toZhMessage(err.message || "提交分析失败");
  } finally {
    analyzeLoading.value = false;
  }
}

async function loadMineAnalyses() {
  try {
    const data = await apiRequest("/analyses/mine?page=0&size=20");
    mineItems.value = data.items || [];
    mineMessage.value = `总数：${data.total ?? 0}`;
  } catch (err) {
    mineMessage.value = toZhMessage(err.message || "加载失败");
  }
}

async function loadAdminUsers() {
  try {
    const keyword = encodeURIComponent(adminUserQuery.keyword || "");
    const data = await apiRequest(`/admin/users?page=0&size=50&keyword=${keyword}`);
    adminUsers.value = data.items || [];
    adminUsersMessage.value = `总数：${data.total ?? 0}`;
  } catch (err) {
    adminUsersMessage.value = toZhMessage(err.message || "加载用户失败");
  }
}

async function updateUserFlags(userId, payload) {
  adminUserLoadingId.value = userId;
  adminUsersMessage.value = "";
  try {
    await apiRequest(`/admin/users/${userId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    await loadAdminUsers();
    adminUsersMessage.value = "用户状态更新成功";
  } catch (err) {
    adminUsersMessage.value = toZhMessage(err.message || "用户状态更新失败");
  } finally {
    adminUserLoadingId.value = null;
  }
}

async function toggleVip(user) {
  await updateUserFlags(user.id, { vip: !user.vip });
}

async function toggleBlacklist(user) {
  await updateUserFlags(user.id, { blacklisted: !user.blacklisted });
}

async function loadConfig() {
  try {
    const data = await apiRequest("/admin/config");
    configForm.baseUrl = data.baseUrl || "";
    configForm.apiKey = data.apiKey || "";
    configForm.model = data.model || "";
    configUpdatedAt.value = data.updatedAt || "";
    configMessage.value = "";
  } catch (err) {
    configMessage.value = toZhMessage(err.message || "加载配置失败");
  }
}

async function loadLlmDefaultsFromConfig() {
  try {
    const data = await apiRequest("/admin/config");
    if (!kbLlmForm.baseUrl) {
      kbLlmForm.baseUrl = data.baseUrl || "";
    }
    if (!kbLlmForm.apiKey) {
      kbLlmForm.apiKey = data.apiKey || "";
    }
    if (!kbLlmForm.model) {
      kbLlmForm.model = data.model || "";
    }
  } catch {
    // ignore
  }
}

async function saveConfig() {
  configLoading.value = true;
  configMessage.value = "";
  try {
    const data = await apiRequest("/admin/config", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(configForm)
    });
    configUpdatedAt.value = data.updatedAt || "";
    configMessage.value = "配置保存成功";
  } catch (err) {
    configMessage.value = toZhMessage(err.message || "保存配置失败");
  } finally {
    configLoading.value = false;
  }
}

async function loadAdminAnalyses() {
  try {
    const query = encodeURIComponent(adminQuery.username || "");
    const data = await apiRequest(`/admin/analyses?page=0&size=20&username=${query}`);
    adminItems.value = data.items || [];
    adminMessage.value = `总数：${data.total ?? 0}`;
  } catch (err) {
    adminMessage.value = toZhMessage(err.message || "加载客户数据失败");
  }
}

async function loadKbDocs() {
  try {
    const data = await apiRequest("/admin/interview-kb/docs?page=0&size=50");
    kbDocs.value = data.items || [];
    kbMessage.value = `文档数：${data.total ?? 0}`;
  } catch (err) {
    kbMessage.value = toZhMessage(err.message || "加载面试题库失败");
  }
}

async function openKbDocViewer(item) {
  kbViewer.visible = true;
  kbViewer.docId = item?.id ?? null;
  kbViewer.title = item?.title || "";
  kbViewer.filename = item?.filename || "";
  kbViewer.total = item?.questionCount ?? 0;
  kbViewer.page = 0;
  kbViewer.size = 100;
  kbViewer.questions = [];
  kbViewMessage.value = "";
  await loadKbDocViewer(true);
}

function closeKbDocViewer() {
  kbViewer.visible = false;
  kbViewer.docId = null;
  kbViewer.title = "";
  kbViewer.filename = "";
  kbViewer.total = 0;
  kbViewer.page = 0;
  kbViewer.questions = [];
  kbViewMessage.value = "";
}

async function loadKbDocViewer(reset = false) {
  if (!kbViewer.docId) return;
  kbViewLoading.value = true;
  kbViewMessage.value = "";
  try {
    const page = reset ? 0 : kbViewer.page;
    const data = await apiRequest(`/admin/interview-kb/docs/${kbViewer.docId}/questions?page=${page}&size=${kbViewer.size}`);
    const rows = Array.isArray(data.questions) ? data.questions : [];
    kbViewer.title = data.title || kbViewer.title;
    kbViewer.filename = data.filename || kbViewer.filename;
    kbViewer.total = data.total ?? kbViewer.total;
    if (reset) {
      kbViewer.questions = rows;
    } else {
      kbViewer.questions.push(...rows);
    }
    kbViewer.page = (data.page ?? page) + 1;
    kbViewMessage.value = kbViewer.total > 0
      ? `已加载 ${kbViewer.questions.length} / ${kbViewer.total} 题`
      : "当前文件没有可预览题目";
  } catch (err) {
    kbViewMessage.value = toZhMessage(err.message || "加载文件内容失败");
  } finally {
    kbViewLoading.value = false;
  }
}

async function loadKbDocViewerMore() {
  if (!kbViewerHasMore.value) return;
  await loadKbDocViewer(false);
}

async function uploadKbDoc() {
  if (!kbUploadForm.file) {
    kbMessage.value = "请先选择面试文档";
    return;
  }
  kbUploadLoading.value = true;
  kbMessage.value = "";
  kbCrawlErrors.value = [];
  try {
    const fd = new FormData();
    fd.append("file", kbUploadForm.file);
    fd.append("title", kbUploadForm.title || "");
    const res = await fetch(`${apiBase}/admin/interview-kb/docs`, {
      method: "POST",
      headers: authHeaders(),
      body: fd
    });
    let data = {};
    try {
      data = await res.json();
    } catch {
      data = {};
    }
    if (!res.ok) {
      throw new Error(toZhMessage(data?.detail || `上传失败: ${res.status}`));
    }
    const successMsg = `已入库：${data.title || data.filename || "新文档"}，题目数：${data.questionCount ?? "-"}`;
    kbUploadForm.title = "";
    kbUploadForm.file = null;
    await loadKbDocs();
    kbMessage.value = successMsg;
  } catch (err) {
    kbMessage.value = toZhMessage(err.message || "上传面试题库失败");
  } finally {
    kbUploadLoading.value = false;
  }
}

async function crawlKbDocs() {
  if (!kbCrawlForm.seedUrl) {
    kbMessage.value = "请填写爬虫种子链接";
    return;
  }
  kbCrawlLoading.value = true;
  kbMessage.value = "";
  kbCrawlErrors.value = [];
  try {
    const payload = {
      seedUrl: kbCrawlForm.seedUrl,
      title: kbCrawlForm.title || "",
      topic: kbCrawlForm.topic || "",
      maxPages: Number.isFinite(Number(kbCrawlForm.maxPages)) ? Number(kbCrawlForm.maxPages) : 20,
      maxDepth: Number.isFinite(Number(kbCrawlForm.maxDepth)) ? Number(kbCrawlForm.maxDepth) : 1,
      sameDomainOnly: !!kbCrawlForm.sameDomainOnly,
      authCookie: kbCrawlForm.authCookie || "",
      referer: kbCrawlForm.referer || "",
      authHeader: kbCrawlForm.authHeader || ""
    };
    const data = await apiRequest("/admin/interview-kb/crawl", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    kbCrawlErrors.value = Array.isArray(data.errors) ? data.errors : [];
    const failedPages = kbCrawlErrors.value.length;
    const successMsg =
      `爬取完成：访问 ${data.pagesVisited ?? 0} 页，成功 ${data.pagesSucceeded ?? 0} 页，` +
      `抽取 ${data.questionsExtracted ?? 0} 条，入库 ${data.questionsImported ?? 0} 条` +
      (failedPages ? `，失败页面 ${failedPages} 个` : "");

    await loadKbDocs();
    kbMessage.value = successMsg;
  } catch (err) {
    kbMessage.value = toZhMessage(err.message || "爬虫入库失败");
  } finally {
    kbCrawlLoading.value = false;
  }
}

async function llmImportKbDocs() {
  if (!kbLlmForm.topic) {
    kbMessage.value = "请填写知识主题";
    return;
  }
  if (!kbLlmForm.baseUrl) {
    kbMessage.value = "请填写模型接口 URL";
    return;
  }
  if (!kbLlmForm.apiKey) {
    kbMessage.value = "请填写模型 API Key";
    return;
  }
  kbLlmLoading.value = true;
  kbMessage.value = "";
  kbCrawlErrors.value = [];
  try {
    const payload = {
      topic: kbLlmForm.topic,
      title: kbLlmForm.title || "",
      baseUrl: kbLlmForm.baseUrl,
      apiKey: kbLlmForm.apiKey,
      model: kbLlmForm.model || "",
      questionCount: Number.isFinite(Number(kbLlmForm.questionCount)) ? Number(kbLlmForm.questionCount) : 30
    };
    const data = await apiRequest("/admin/interview-kb/llm-import", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    const successMsg =
      `模型入库完成：主题 ${data.topic || "-"}，请求 ${data.requestedCount ?? 0} 题，` +
      `生成 ${data.generatedCount ?? 0} 题，入库 ${data.importedCount ?? 0} 题`;
    await loadKbDocs();
    kbMessage.value = successMsg;
  } catch (err) {
    kbMessage.value = toZhMessage(err.message || "模型入库失败");
  } finally {
    kbLlmLoading.value = false;
  }
}

async function deleteKbDoc(docId) {
  kbUploadLoading.value = true;
  kbMessage.value = "";
  kbCrawlErrors.value = [];
  try {
    await apiRequest(`/admin/interview-kb/docs/${docId}`, { method: "DELETE" });
    if (kbViewer.docId === docId) {
      closeKbDocViewer();
    }
    kbMessage.value = "文档已删除";
    await loadKbDocs();
  } catch (err) {
    kbMessage.value = toZhMessage(err.message || "删除失败");
  } finally {
    kbUploadLoading.value = false;
  }
}

async function switchMine() {
  tab.value = "mine";
  await loadMineAnalyses();
}

async function switchAdminUsers() {
  tab.value = "adminUsers";
  await loadAdminUsers();
}

async function switchAdminConfig() {
  tab.value = "adminConfig";
  await loadConfig();
}

async function switchAdminData() {
  tab.value = "adminData";
  await loadAdminAnalyses();
}

async function switchAdminKb() {
  tab.value = "adminKb";
  await loadKbDocs();
  await loadLlmDefaultsFromConfig();
}

function formatTime(v) {
  if (!v) return "-";
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return v;
  return d.toLocaleString();
}

onMounted(async () => {
  if (!token.value) return;
  try {
    await loadMe();
  } catch {
    await logout();
  }
});

onBeforeUnmount(() => {
  stopPolling();
});
</script>
