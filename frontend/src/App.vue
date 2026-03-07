<template>
  <!-- 根容器：未登录时使用 page-auth 样式 -->
  <div class="page" :class="{ 'page-auth': !token }">
    <!-- ===== 登录/注册页面 ===== -->
    <section v-if="!token" class="auth-screen">
      <div class="auth-shell">
        <section class="auth-showcase">
          <div class="auth-showcase-copy">
            <span class="eyebrow">Resume AI Workspace</span>
            <h1>让简历优化、岗位匹配与管理协同落在同一张工作台上</h1>
            <p>
              参考国际大厂产品常见的信息分层与留白节奏，保留你现有功能，重做工作台骨架，
              让分析路径、结果反馈和管理入口都更清楚。
            </p>
          </div>

          <div class="auth-highlight-grid">
            <article v-for="item in authHighlights" :key="item.title" class="auth-highlight-card">
              <span>{{ item.tag }}</span>
              <h3>{{ item.title }}</h3>
              <p>{{ item.desc }}</p>
            </article>
          </div>
        </section>

        <div class="auth-wrapper">
          <form class="auth-form" @submit.prevent="submitAuth">
            <span class="auth-badge">{{ authMode === 'login' ? '欢迎回来' : '创建账号' }}</span>
            <h2>{{ authMode === 'login' ? '登录工作台' : '注册新账号' }}</h2>
            <p class="auth-subtitle">
              {{ authMode === 'login' ? '继续你的简历分析与管理任务。' : '几秒内完成注册，开始体验完整分析闭环。' }}
            </p>

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
      </div>
    </section>

    <!-- ===== 登录后主工作区 ===== -->
    <template v-else>
      <div class="workspace">
        <!-- 左侧导航面板 -->
        <aside class="side-panel">
          <div class="brand-block">
            <span class="eyebrow brand-eyebrow">Resume AI Workspace</span>
            <h1>AI 简历分析系统</h1>
            <p>清晰、克制、专业的企业级工作台</p>
          </div>

          <!-- 当前用户信息展示区 -->
          <section class="side-user">
            <div class="side-user-top">
              <div>
                <span class="user-overline">当前账号</span>
                <strong>{{ me.username }}</strong>
              </div>
              <span class="pill">{{ roleText(me.role) }}</span>
            </div>
            <div class="side-user-tags">
              <span v-if="me.vip" class="pill vip-pill">VIP 优先</span>
              <span v-else class="pill">标准优先级</span>
              <span v-if="me.blacklisted" class="pill danger-pill">已拉黑</span>
            </div>
          </section>

          <!-- 功能标签页切换（管理员可见更多标签） -->
          <section class="side-nav-group">
            <p class="side-group-title">工作区导航</p>
            <div class="side-nav-list">
              <button
                v-for="item in workspaceNavItems"
                :key="item.key"
                class="side-nav-item"
                :class="{ active: tab === item.key }"
                @click="openWorkspaceTab(item.key)"
              >
                <span class="side-nav-badge">{{ item.badge }}</span>
                <span class="side-nav-copy">
                  <strong>{{ item.label }}</strong>
                  <small>{{ item.desc }}</small>
                </span>
              </button>
            </div>
          </section>

          <section class="side-insight">
            <p class="side-group-title">当前状态</p>
            <div class="side-insight-list">
              <div v-for="item in sidebarInsights" :key="item.label" class="side-insight-item">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </section>

          <p class="side-note">VIP 用户任务会被优先消费</p>
        </aside>

        <!-- 右侧主内容区 -->
        <main class="content-panel">
          <!-- 页面标题栏与退出按钮 -->
          <section class="main-head workspace-hero">
            <div class="hero-copy">
              <span class="eyebrow">{{ currentTabMeta.kicker }}</span>
              <h2>{{ currentTabMeta.title }}</h2>
              <p>{{ currentTabMeta.description }}</p>
              <div class="hero-tags">
                <span class="hero-tag">{{ me.vip ? 'VIP 优先队列' : '标准优先级' }}</span>
                <span class="hero-tag">{{ currentTabMeta.tag }}</span>
                <span v-if="queueJob.jobId" class="hero-tag">{{ statusText(queueJob.status) }}</span>
              </div>
            </div>
            <div class="hero-actions">
              <button
                v-if="tab === 'analyze' && analyzeSubtab !== 'analysis'"
                class="ghost secondary"
                @click="analyzeSubtab = 'analysis'"
              >
                返回基础分析
              </button>
              <button class="ghost" @click="logout">退出登录</button>
            </div>
          </section>

          <section class="overview-grid">
            <article v-for="item in overviewStats" :key="item.label" class="overview-card">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <p>{{ item.desc }}</p>
            </article>
          </section>

          <!-- ===== 简历分析标签页 ===== -->
          <section v-if="tab === 'analyze'" class="card">
        <div class="analyze-header">
          <div class="section-text">
            <span class="section-kicker">Core Workflow</span>
            <h2>简历分析工作台</h2>
            <p>先完成一次基础分析，再进入生成、对话、雷达图和真实性检测，形成完整优化闭环。</p>
          </div>
          <div class="section-hint">
            <span>{{ currentAnalyzeMeta.label }}</span>
            <p>{{ currentAnalyzeMeta.desc }}</p>
          </div>
        </div>

        <div class="analyze-subtabs">
          <button
            v-for="item in analyzeTabItems"
            :key="item.key"
            :class="{ active: analyzeSubtab === item.key }"
            @click="analyzeSubtab = item.key"
          >
            <span>{{ item.label }}</span>
            <small>{{ item.desc }}</small>
          </button>
        </div>

        <ResumeGeneratorGroup
          v-if="analyzeSubtab === 'generate'"
          :form="resumeGenForm"
          :loading="resumeGenLoading"
          :message="resumeGenMessage"
          :generated-resume="generatedResume"
          :can-rewrite="!!result?.analysisId"
          @submit-generate="generateResumeFromJd"
          @rewrite="rewriteResumeFromCurrentAnalysis"
          @copy="copyGeneratedResume"
          @download="downloadGeneratedResume"
        />

        <template v-if="analyzeSubtab === 'analysis'">
          <AnalyzeSubmissionGroup
            :form="analyzeForm"
            :loading="analyzeLoading"
            :message="analyzeMessage"
            :queue-job="queueJob"
            :status-text="statusText"
            :status-class="statusClass"
            @submit-analyze="submitAnalyze"
            @file-change="onFileChange"
            @jd-image-change="onJdImageChange"
            @refresh-job="refreshCurrentJob"
          />

          <div v-if="result" class="result">
            <AnalysisResultSummaryGroup
              :result="result"
              @copy-resume="copyText(result.optimizedResumeMarkdown)"
              @download-resume="downloadText('optimized-resume.md', result.optimizedResumeMarkdown)"
            />
          </div>
        </template>

        <div v-if="result" class="result">
          <ResumeChatGroup
            v-if="analyzeSubtab === 'chat'"
            :chat-state="chatState"
            :loading="chatLoading"
            :message="chatMessage"
            :can-start="!!result.analysisId"
            @start="startResumeChat"
            @send="sendChatMessage"
            @quick-ask="quickAsk"
          />

          <JdRadarGroup
            v-if="analyzeSubtab === 'radar'"
            :loading="radarLoading"
            :message="radarMessage"
            :data="jdRadar"
            :can-run="!!result.analysisId"
            @run="runJdRadarFromCurrent"
          />

          <ResumeAuditGroup
            v-if="analyzeSubtab === 'audit'"
            :loading="auditLoading"
            :message="auditMessage"
            :data="resumeAudit"
            :can-run="!!result.analysisId"
            @run="runAuditFromCurrent"
          />

          <MockInterviewGroup
            v-if="analyzeSubtab === 'interview'"
            :loading="interviewLoading"
            :message="interviewMessage"
            :interview-state="interviewState"
            :format-score="formatScore"
            @start="startInterview"
            @answer="answerQuestion"
            @end="endInterview"
          />

          <div v-if="analyzeSubtab === 'recommend'" class="sub-card">
            <h3>岗位推荐</h3>
            <p class="message">{{ recommendMessage }}</p>
            <div class="input-field">
              <textarea v-model.trim="recommendState.resumeText" rows="10" placeholder="粘贴简历文本" required></textarea>
            </div>
            <button @click="recommendJobs" :disabled="recommendLoading">
              {{ recommendLoading ? '推荐中...' : '获取推荐岗位' }}
            </button>
            <div v-if="recommendState.recommendations.length" class="recommendations-list">
              <h4>推荐结果</h4>
              <div v-for="(rec, idx) in recommendState.recommendations" :key="idx" class="recommendation-item">
                <h5>{{ rec.title }}</h5>
                <p>{{ rec.company }}</p>
                <p>匹配度：{{ formatScore(rec.matchScore) }}</p>
                <p>{{ rec.description }}</p>
              </div>
            </div>
          </div>

          <div v-if="analyzeSubtab === 'batch'" class="sub-card">
            <h3>批量分析</h3>
            <p class="message">{{ batchMessage }}</p>
            <div class="input-field">
              <label>选择多份简历文件</label>
              <input type="file" multiple @change="batchState.files = Array.from($event.target.files)" />
            </div>
            <div class="input-field">
              <label>岗位描述 JD（可选）</label>
              <textarea v-model.trim="batchState.jdText" rows="4" placeholder="粘贴 JD 文本"></textarea>
            </div>
            <div class="input-field">
              <label>目标岗位（可选）</label>
              <input v-model.trim="batchState.targetRole" type="text" placeholder="例如：Java 高级后端工程师" />
            </div>
            <button @click="submitBatch" :disabled="batchLoading">
              {{ batchLoading ? '提交中...' : '提交批量任务' }}
            </button>
            <div v-if="batchState.batchId" class="batch-status">
              <h4>批量任务状态：{{ batchState.status }}</h4>
              <div v-if="batchState.jobs.length" class="batch-jobs">
                <table class="dense-table">
                  <thead>
                    <tr>
                      <th>文件名</th>
                      <th>状态</th>
                      <th>评分</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="job in batchState.jobs" :key="job.jobId">
                      <td>{{ job.filename }}</td>
                      <td>{{ job.status }}</td>
                      <td>{{ job.score != null ? formatScore(job.score) : '-' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        <p v-if="analyzeSubtab !== 'analysis' && !result" class="message">
          请先在“基础分析”里完成一次简历分析，再使用当前功能。
        </p>
      </section>

      <!-- ===== 我的简历记录标签页 ===== -->
      <section v-else-if="tab === 'mine'" class="card">
        <div class="panel-head">
          <div class="section-text">
            <span class="section-kicker">History Center</span>
            <h2>我的简历记录</h2>
            <p>按时间回看你的分析资产，快速找到最近岗位、得分变化和可复用建议。</p>
          </div>
          <div class="panel-actions">
            <button class="ghost secondary" @click="loadMineAnalyses">刷新记录</button>
          </div>
        </div>

        <div class="stat-strip">
          <article v-for="item in mineStatItems" :key="item.label" class="stat-chip">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>

        <div class="toolbar-shell">
          <div class="toolbar-search">
            <input v-model.trim="mineLocalKeyword" type="text" placeholder="按文件名、岗位或建议筛选" />
            <button v-if="mineLocalKeyword" class="ghost secondary" @click="mineLocalKeyword = ''">清空</button>
          </div>
          <div class="toolbar-meta">
            <span class="record-tip">按时间倒序展示，重点建议可展开查看</span>
            <p class="message compact">{{ mineMessage }}</p>
          </div>
        </div>
        <div class="table-wrap">
          <table class="lux-table mine-table dense-table">
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
              <tr v-if="!filteredMineItems.length">
                <td colspan="7" class="table-empty">
                  <strong>{{ mineItems.length ? '没有符合条件的记录' : '还没有分析记录' }}</strong>
                  <span>{{ mineItems.length ? '试试更换筛选关键词。' : '先去“简历分析”提交一次任务，这里会自动沉淀历史结果。' }}</span>
                </td>
              </tr>
              <tr v-for="item in filteredMineItems" :key="item.id">
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
        <div class="pagination-bar" v-if="mineTotal > 0">
          <div class="pagination-size">
            <span>每页</span>
            <select :value="mineSize" @change="mineChangeSize(Number($event.target.value))">
              <option v-for="s in pageSizeOptions" :key="s" :value="s">{{ s }}</option>
            </select>
            <span>条</span>
          </div>
          <div class="pagination-pages">
            <button class="pg-btn" :disabled="minePage === 0" @click="mineGoPage(0)">&laquo;</button>
            <button class="pg-btn" :disabled="minePage === 0" @click="mineGoPage(minePage - 1)">&lsaquo;</button>
            <button v-for="p in pageRange(minePage, mineTotalPages)" :key="p" class="pg-btn" :class="{ active: p === minePage }" @click="mineGoPage(p)">{{ p + 1 }}</button>
            <button class="pg-btn" :disabled="minePage >= mineTotalPages - 1" @click="mineGoPage(minePage + 1)">&rsaquo;</button>
            <button class="pg-btn" :disabled="minePage >= mineTotalPages - 1" @click="mineGoPage(mineTotalPages - 1)">&raquo;</button>
          </div>
          <span class="pagination-total">共 {{ mineTotal }} 条</span>
        </div>
      </section>

      <!-- ===== 管理员 - 用户管理标签页 ===== -->
      <section v-else-if="tab === 'adminUsers'" class="card">
        <div class="panel-head">
          <div class="section-text">
            <span class="section-kicker">User Operations</span>
            <h2>管理员 - 用户管理</h2>
            <p>统一处理检索、权限状态和风险控制，让用户治理更接近标准后台工作流。</p>
          </div>
          <div class="panel-actions">
            <button class="ghost secondary" @click="loadAdminUsers">刷新列表</button>
          </div>
        </div>

        <div class="stat-strip">
          <article v-for="item in adminUserStatItems" :key="item.label" class="stat-chip">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>

        <div class="toolbar-shell">
          <div class="toolbar-search">
            <input v-model.trim="adminUserQuery.keyword" type="text" placeholder="按用户名搜索" />
            <button @click="adminUserPage = 0; loadAdminUsers()">搜索</button>
            <button
              v-if="adminUserQuery.keyword"
              class="ghost secondary"
              @click="adminUserQuery.keyword = ''; loadAdminUsers()"
            >
              清空
            </button>
          </div>
          <div class="toolbar-meta">
            <span class="record-tip">可直接批量巡检账号状态、VIP 和黑名单</span>
            <p class="message compact">{{ adminUsersMessage }}</p>
          </div>
        </div>
        <div class="table-wrap">
          <table class="dense-table">
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
              <tr v-if="!adminUsers.length">
                <td colspan="8" class="table-empty">
                  <strong>没有找到用户</strong>
                  <span>请调整搜索关键词，或刷新后再试。</span>
                </td>
              </tr>
              <tr v-for="item in adminUsers" :key="item.id">
                <td>{{ item.id }}</td>
                <td>{{ item.username }}</td>
                <td><span class="role-tag">{{ roleText(item.role) }}</span></td>
                <td>
                  <span class="state-pill" :class="item.vip ? 'state-yes' : 'state-no'">
                    {{ item.vip ? 'VIP' : '普通' }}
                  </span>
                </td>
                <td>
                  <span class="state-pill" :class="item.blacklisted ? 'state-no' : 'state-yes'">
                    {{ item.blacklisted ? '已拉黑' : '正常' }}
                  </span>
                </td>
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
        <div class="pagination-bar" v-if="adminUserTotal > 0">
          <div class="pagination-size">
            <span>每页</span>
            <select :value="adminUserSize" @change="adminUserChangeSize(Number($event.target.value))">
              <option v-for="s in pageSizeOptions" :key="s" :value="s">{{ s }}</option>
            </select>
            <span>条</span>
          </div>
          <div class="pagination-pages">
            <button class="pg-btn" :disabled="adminUserPage === 0" @click="adminUserGoPage(0)">&laquo;</button>
            <button class="pg-btn" :disabled="adminUserPage === 0" @click="adminUserGoPage(adminUserPage - 1)">&lsaquo;</button>
            <button v-for="p in pageRange(adminUserPage, adminUserTotalPages)" :key="p" class="pg-btn" :class="{ active: p === adminUserPage }" @click="adminUserGoPage(p)">{{ p + 1 }}</button>
            <button class="pg-btn" :disabled="adminUserPage >= adminUserTotalPages - 1" @click="adminUserGoPage(adminUserPage + 1)">&rsaquo;</button>
            <button class="pg-btn" :disabled="adminUserPage >= adminUserTotalPages - 1" @click="adminUserGoPage(adminUserTotalPages - 1)">&raquo;</button>
          </div>
          <span class="pagination-total">共 {{ adminUserTotal }} 条</span>
        </div>
      </section>

      <!-- ===== 管理员 - 模型配置标签页 ===== -->
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

      <!-- ===== 管理员 - 客户简历数据标签页 ===== -->
      <section v-else-if="tab === 'adminData'" class="card">
        <h2>管理员 - 客户简历数据</h2>
        <div class="inline">
          <input v-model.trim="adminQuery.username" type="text" placeholder="按用户名搜索" />
          <button @click="adminPage = 0; loadAdminAnalyses()">搜索</button>
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
        <div class="pagination-bar" v-if="adminTotal > 0">
          <div class="pagination-size">
            <span>每页</span>
            <select :value="adminSize" @change="adminChangeSize(Number($event.target.value))">
              <option v-for="s in pageSizeOptions" :key="s" :value="s">{{ s }}</option>
            </select>
            <span>条</span>
          </div>
          <div class="pagination-pages">
            <button class="pg-btn" :disabled="adminPage === 0" @click="adminGoPage(0)">&laquo;</button>
            <button class="pg-btn" :disabled="adminPage === 0" @click="adminGoPage(adminPage - 1)">&lsaquo;</button>
            <button v-for="p in pageRange(adminPage, adminTotalPages)" :key="p" class="pg-btn" :class="{ active: p === adminPage }" @click="adminGoPage(p)">{{ p + 1 }}</button>
            <button class="pg-btn" :disabled="adminPage >= adminTotalPages - 1" @click="adminGoPage(adminPage + 1)">&rsaquo;</button>
            <button class="pg-btn" :disabled="adminPage >= adminTotalPages - 1" @click="adminGoPage(adminTotalPages - 1)">&raquo;</button>
          </div>
          <span class="pagination-total">共 {{ adminTotal }} 条</span>
        </div>
      </section>

      <!-- ===== 管理员 - 面试题库标签页 ===== -->
      <section v-else-if="tab === 'adminKb'" class="card">
        <div class="panel-head">
          <div class="section-text">
            <span class="section-kicker">Knowledge Operations</span>
            <h2>管理员 - 面试题库</h2>
            <p>把上传、爬取与模型生成聚合到同一页，同时补上文档筛选、统计和空状态反馈。</p>
          </div>
          <div class="panel-actions">
            <button class="ghost secondary" @click="loadKbDocs">刷新题库</button>
          </div>
        </div>

        <div class="stat-strip">
          <article v-for="item in kbStatItems" :key="item.label" class="stat-chip">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>

        <div class="kb-grid">
          <!-- 方式一：手动上传面试文档 -->
          <form class="kb-block" @submit.prevent="uploadKbDoc">
            <h3>文档上传入库</h3>
            <label>文档标题（可选）</label>
            <input v-model.trim="kbUploadForm.title" type="text" placeholder="例如：Java后端高频面试题（2026）" />

            <label>上传面试文档（pdf/docx/txt/md/png/jpg）</label>
            <input type="file" accept=".pdf,.docx,.txt,.md,.png,.jpg,.jpeg,.bmp,.webp,.tif,.tiff" @change="onKbFileChange" required />

            <button :disabled="kbUploadLoading || kbCrawlLoading || kbLlmLoading">{{ kbUploadLoading ? '上传中...' : '上传并入库' }}</button>
          </form>

          <!-- 方式二：爬虫自动抓取入库 -->
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

          <!-- 方式三：大模型生成面试题入库 -->
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
        <div class="toolbar-shell toolbar-shell-top-gap">
          <div class="toolbar-search">
            <input v-model.trim="kbLocalKeyword" type="text" placeholder="按标题、文件名或上传人筛选当前列表" />
            <button v-if="kbLocalKeyword" class="ghost secondary" @click="kbLocalKeyword = ''">清空</button>
          </div>
          <div class="toolbar-meta">
            <span class="record-tip">支持对当前已加载文档做本地快速筛选</span>
            <p class="message compact">{{ kbMessage }}</p>
          </div>
        </div>
        <details v-if="kbCrawlErrors.length" class="detail">
          <summary>本次爬取失败页面（{{ kbCrawlErrors.length }}）</summary>
          <pre>{{ kbCrawlErrors.join('\n') }}</pre>
        </details>
        <div class="table-wrap">
          <table class="dense-table">
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
              <tr v-if="!filteredKbDocs.length">
                <td colspan="7" class="table-empty">
                  <strong>{{ kbDocs.length ? '没有符合条件的题库文档' : '题库中还没有文档' }}</strong>
                  <span>{{ kbDocs.length ? '试试更换筛选关键词。' : '可以从上传、爬取或模型生成三种入口开始构建题库。' }}</span>
                </td>
              </tr>
              <tr v-for="item in filteredKbDocs" :key="item.id">
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
        <div class="pagination-bar" v-if="kbDocTotal > 0">
          <div class="pagination-size">
            <span>每页</span>
            <select :value="kbDocSize" @change="kbDocChangeSize(Number($event.target.value))">
              <option v-for="s in pageSizeOptions" :key="s" :value="s">{{ s }}</option>
            </select>
            <span>条</span>
          </div>
          <div class="pagination-pages">
            <button class="pg-btn" :disabled="kbDocPage === 0" @click="kbDocGoPage(0)">&laquo;</button>
            <button class="pg-btn" :disabled="kbDocPage === 0" @click="kbDocGoPage(kbDocPage - 1)">&lsaquo;</button>
            <button v-for="p in pageRange(kbDocPage, kbDocTotalPages)" :key="p" class="pg-btn" :class="{ active: p === kbDocPage }" @click="kbDocGoPage(p)">{{ p + 1 }}</button>
            <button class="pg-btn" :disabled="kbDocPage >= kbDocTotalPages - 1" @click="kbDocGoPage(kbDocPage + 1)">&rsaquo;</button>
            <button class="pg-btn" :disabled="kbDocPage >= kbDocTotalPages - 1" @click="kbDocGoPage(kbDocTotalPages - 1)">&raquo;</button>
          </div>
          <span class="pagination-total">共 {{ kbDocTotal }} 条</span>
        </div>

        <!-- 题库文档内容预览弹窗 -->
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

      <!-- ===== 简历版本管理标签页 ===== -->
      <section v-else-if="tab === 'versions'" class="card">
        <div class="panel-head">
          <div class="section-text">
            <span class="section-kicker">Version Control</span>
            <h2>简历版本管理</h2>
            <p>保存、对比不同版本的简历，追踪优化历程。</p>
          </div>
          <div class="panel-actions">
            <button class="ghost secondary" @click="loadVersions(versionsState.page)">刷新列表</button>
            <button @click="versionsState.compareMode = !versionsState.compareMode">
              {{ versionsState.compareMode ? '退出对比' : '版本对比' }}
            </button>
          </div>
        </div>

        <p class="message">{{ versionsMessage }}</p>

        <div class="table-wrap">
          <table class="dense-table">
            <thead>
              <tr>
                <th v-if="versionsState.compareMode">选择</th>
                <th>ID</th>
                <th>版本名称</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!versionsState.items.length">
                <td :colspan="versionsState.compareMode ? 5 : 4" class="table-empty">
                  <strong>还没有保存的版本</strong>
                  <span>在分析结果页保存简历版本后，这里会显示历史版本。</span>
                </td>
              </tr>
              <tr v-for="item in versionsState.items" :key="item.id">
                <td v-if="versionsState.compareMode">
                  <input type="checkbox" :value="item.id"
                    :checked="versionsState.compareId1 === item.id || versionsState.compareId2 === item.id"
                    @change="toggleCompareVersion(item.id)" />
                </td>
                <td>#{{ item.id }}</td>
                <td>{{ item.name }}</td>
                <td>{{ formatTime(item.createdAt) }}</td>
                <td>
                  <button class="mini-btn" @click="versionsState.selectedVersion = item">查看</button>
                  <button class="mini-btn warn" @click="deleteVersion(item.id)" :disabled="versionsLoading">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="versionsState.compareMode && versionsState.compareId1 && versionsState.compareId2" class="sub-card">
          <button @click="compareVersions(versionsState.compareId1, versionsState.compareId2)" :disabled="versionsLoading">
            执行对比
          </button>
          <div v-if="versionsState.compareResult" class="compare-result">
            <h3>对比结果</h3>
            <pre>{{ versionsState.compareResult }}</pre>
          </div>
        </div>

        <div class="pagination-bar" v-if="versionsState.total > 0">
          <button :disabled="versionsState.page === 0" @click="loadVersions(versionsState.page - 1)">上一页</button>
          <span>第 {{ versionsState.page + 1 }} / {{ versionsState.totalPages }} 页</span>
          <button :disabled="versionsState.page >= versionsState.totalPages - 1" @click="loadVersions(versionsState.page + 1)">下一页</button>
          <span>共 {{ versionsState.total }} 条</span>
        </div>
      </section>

      <!-- ===== 数据统计标签页 ===== -->
      <section v-else-if="tab === 'stats'" class="card">
        <div class="panel-head">
          <div class="section-text">
            <span class="section-kicker">Analytics</span>
            <h2>数据统计</h2>
            <p>查看个人分析历史、评分趋势与关键词匹配情况。</p>
          </div>
          <div class="panel-actions">
            <button class="ghost secondary" @click="loadUserStats">刷新数据</button>
          </div>
        </div>

        <p class="message">{{ userStatsMessage }}</p>

        <div class="stat-strip">
          <article class="stat-chip">
            <span>总分析数</span>
            <strong>{{ userStatsState.totalAnalyses }} 次</strong>
          </article>
          <article class="stat-chip">
            <span>平均评分</span>
            <strong>{{ formatScore(userStatsState.avgScore) }}</strong>
          </article>
        </div>

        <div class="sub-card">
          <h3>评分历史趋势</h3>
          <div v-if="userStatsState.scoreHistory.length" class="chart-placeholder">
            <p v-for="(item, idx) in userStatsState.scoreHistory" :key="idx">
              {{ formatCompactTime(item.date) }}: {{ formatScore(item.score) }}
            </p>
          </div>
          <p v-else>暂无评分历史数据</p>
        </div>

        <div class="sub-card">
          <h3>高频匹配关键词</h3>
          <div v-if="userStatsState.topMatchedKeywords.length">
            <span v-for="kw in userStatsState.topMatchedKeywords" :key="kw" class="keyword-tag">{{ kw }}</span>
          </div>
          <p v-else>暂无数据</p>
        </div>

        <div class="sub-card">
          <h3>常见缺失关键词</h3>
          <div v-if="userStatsState.topMissingKeywords.length">
            <span v-for="kw in userStatsState.topMissingKeywords" :key="kw" class="keyword-tag warn">{{ kw }}</span>
          </div>
          <p v-else>暂无数据</p>
        </div>
      </section>

      <!-- ===== 管理员 - 操作日志标签页 ===== -->
      <section v-else-if="tab === 'adminAudit'" class="card">
        <div class="panel-head">
          <div class="section-text">
            <span class="section-kicker">Audit Trail</span>
            <h2>管理员 - 操作日志</h2>
            <p>追踪管理员操作记录，确保系统安全与合规。</p>
          </div>
          <div class="panel-actions">
            <button class="ghost secondary" @click="loadAuditLogs(auditLogsState.page)">刷新日志</button>
          </div>
        </div>

        <p class="message">{{ auditLogsMessage }}</p>

        <div class="toolbar-shell">
          <div class="toolbar-search">
            <input v-model.trim="auditLogsState.filters.action" type="text" placeholder="操作类型" />
            <input v-model.trim="auditLogsState.filters.adminUsername" type="text" placeholder="管理员用户名" />
            <input v-model.trim="auditLogsState.filters.from" type="date" placeholder="开始日期" />
            <input v-model.trim="auditLogsState.filters.to" type="date" placeholder="结束日期" />
            <button @click="auditLogsState.page = 0; loadAuditLogs(0)">搜索</button>
            <button class="ghost secondary" @click="auditLogsState.filters = { action: '', adminUsername: '', from: '', to: '' }; loadAuditLogs(0)">清空</button>
          </div>
        </div>

        <div class="table-wrap">
          <table class="dense-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>操作</th>
                <th>管理员</th>
                <th>目标</th>
                <th>详情</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!auditLogsState.items.length">
                <td colspan="6" class="table-empty">
                  <strong>暂无审计日志</strong>
                </td>
              </tr>
              <tr v-for="item in auditLogsState.items" :key="item.id">
                <td>#{{ item.id }}</td>
                <td>{{ item.action }}</td>
                <td>{{ item.adminUsername }}</td>
                <td>{{ item.targetType }} #{{ item.targetId }}</td>
                <td>{{ item.details || '-' }}</td>
                <td>{{ formatTime(item.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination-bar" v-if="auditLogsState.total > 0">
          <button :disabled="auditLogsState.page === 0" @click="loadAuditLogs(auditLogsState.page - 1)">上一页</button>
          <span>第 {{ auditLogsState.page + 1 }} 页</span>
          <button :disabled="(auditLogsState.page + 1) * auditLogsState.size >= auditLogsState.total" @click="loadAuditLogs(auditLogsState.page + 1)">下一页</button>
          <span>共 {{ auditLogsState.total }} 条</span>
        </div>
      </section>

      <!-- ===== 管理员 - 系统统计标签页 ===== -->
      <section v-else-if="tab === 'adminStats'" class="card">
        <div class="panel-head">
          <div class="section-text">
            <span class="section-kicker">System Analytics</span>
            <h2>管理员 - 系统统计</h2>
            <p>查看系统整体运行数据、用户活跃度与分析趋势。</p>
          </div>
          <div class="panel-actions">
            <button class="ghost secondary" @click="loadAdminStats">刷新数据</button>
          </div>
        </div>

        <p class="message">{{ adminStatsMessage }}</p>

        <div class="stat-strip">
          <article class="stat-chip">
            <span>总用户数</span>
            <strong>{{ adminStatsState.totalUsers }} 人</strong>
          </article>
          <article class="stat-chip">
            <span>活跃用户</span>
            <strong>{{ adminStatsState.activeUsers }} 人</strong>
          </article>
          <article class="stat-chip">
            <span>总分析数</span>
            <strong>{{ adminStatsState.totalAnalyses }} 次</strong>
          </article>
          <article class="stat-chip">
            <span>平均评分</span>
            <strong>{{ formatScore(adminStatsState.avgScore) }}</strong>
          </article>
        </div>

        <div class="sub-card">
          <h3>每日分析量</h3>
          <div v-if="adminStatsState.dailyAnalysisCounts.length" class="chart-placeholder">
            <p v-for="(item, idx) in adminStatsState.dailyAnalysisCounts" :key="idx">
              {{ item.date }}: {{ item.count }} 次
            </p>
          </div>
          <p v-else>暂无数据</p>
        </div>

        <div class="sub-card">
          <h3>热门岗位</h3>
          <div v-if="adminStatsState.popularRoles.length">
            <p v-for="(item, idx) in adminStatsState.popularRoles" :key="idx">
              {{ item.role }}: {{ item.count }} 次
            </p>
          </div>
          <p v-else>暂无数据</p>
        </div>
      </section>

      <!-- ===== 管理员 - 模板管理标签页 ===== -->
      <section v-else-if="tab === 'adminTemplates'" class="card">
        <div class="panel-head">
          <div class="section-text">
            <span class="section-kicker">Template Library</span>
            <h2>管理员 - 模板管理</h2>
            <p>创建、编辑和管理简历模板库，提升用户体验。</p>
          </div>
          <div class="panel-actions">
            <button class="ghost secondary" @click="loadTemplates(templatesState.page)">刷新列表</button>
            <button @click="templatesState.editingTemplate = { name: '', content: '', category: '' }">新建模板</button>
          </div>
        </div>

        <p class="message">{{ templatesMessage }}</p>

        <div v-if="templatesState.editingTemplate" class="sub-card">
          <h3>{{ templatesState.editingTemplate.id ? '编辑模板' : '新建模板' }}</h3>
          <div class="input-field">
            <input v-model.trim="templatesState.editingTemplate.name" type="text" placeholder="模板名称" required />
          </div>
          <div class="input-field">
            <input v-model.trim="templatesState.editingTemplate.category" type="text" placeholder="分类" />
          </div>
          <div class="input-field">
            <textarea v-model.trim="templatesState.editingTemplate.content" rows="10" placeholder="模板内容" required></textarea>
          </div>
          <button @click="saveTemplate" :disabled="templatesLoading">保存</button>
          <button class="ghost secondary" @click="templatesState.editingTemplate = null">取消</button>
        </div>

        <div class="table-wrap">
          <table class="dense-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>名称</th>
                <th>分类</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!templatesState.items.length">
                <td colspan="5" class="table-empty">
                  <strong>还没有模板</strong>
                  <span>点击"新建模板"创建第一个模板。</span>
                </td>
              </tr>
              <tr v-for="item in templatesState.items" :key="item.id">
                <td>#{{ item.id }}</td>
                <td>{{ item.name }}</td>
                <td>{{ item.category || '-' }}</td>
                <td>{{ formatTime(item.createdAt) }}</td>
                <td>
                  <button class="mini-btn" @click="templatesState.editingTemplate = { ...item }">编辑</button>
                  <button class="mini-btn warn" @click="deleteTemplate(item.id)" :disabled="templatesLoading">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination-bar" v-if="templatesState.total > 0">
          <button :disabled="templatesState.page === 0" @click="loadTemplates(templatesState.page - 1)">上一页</button>
          <span>第 {{ templatesState.page + 1 }} 页</span>
          <button :disabled="(templatesState.page + 1) * templatesState.size >= templatesState.total" @click="loadTemplates(templatesState.page + 1)">下一页</button>
          <span>共 {{ templatesState.total }} 条</span>
        </div>
      </section>
        </main>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, reactive, ref } from "vue";

const AnalyzeSubmissionGroup = defineAsyncComponent(() => import("./components/analyze/AnalyzeSubmissionGroup.vue"));
const AnalysisResultSummaryGroup = defineAsyncComponent(() => import("./components/analyze/AnalysisResultSummaryGroup.vue"));
const JdRadarGroup = defineAsyncComponent(() => import("./components/analyze/JdRadarGroup.vue"));
const ResumeAuditGroup = defineAsyncComponent(() => import("./components/analyze/ResumeAuditGroup.vue"));
const ResumeChatGroup = defineAsyncComponent(() => import("./components/analyze/ResumeChatGroup.vue"));
const ResumeGeneratorGroup = defineAsyncComponent(() => import("./components/analyze/ResumeGeneratorGroup.vue"));
const MockInterviewGroup = defineAsyncComponent(() => import("./components/analyze/MockInterviewGroup.vue"));

// ===== 基础配置 =====
const apiBase = "./api";           // API 请求基础路径（相对路径，由 Nginx 代理到后端）
const tokenKey = "resume_ai_token"; // 本地存储中 token 的键名

// ===== 登录态与当前用户 =====
const tab = ref("analyze");        // 当前激活的标签页
const token = ref(localStorage.getItem(tokenKey) || sessionStorage.getItem(tokenKey) || ""); // 登录令牌
const me = reactive({ id: null, username: "", role: "", vip: false, blacklisted: false }); // 当前登录用户信息
const authLoading = ref(false);    // 登录/注册请求加载状态
const authMessage = ref("");       // 登录/注册提示消息
const authMode = ref("login");     // 当前认证模式：login 或 register
const rememberMe = ref(true);      // 是否记住登录状态

const loginForm = reactive({ username: "", password: "" });    // 登录表单数据
const registerForm = reactive({ username: "", password: "" }); // 注册表单数据

// ===== 简历分析页状态 =====
const analyzeLoading = ref(false); // 分析任务提交加载状态
const analyzeMessage = ref("");    // 分析页提示消息
const result = ref(null);          // 分析结果数据
const analyzeSubtab = ref("analysis");
const analyzeForm = reactive({     // 分析表单数据
  targetRole: "",                  // 目标岗位
  jdText: "",                      // 岗位描述文本
  file: null,                      // 简历文件
  jdImage: null                    // JD 截图文件
});

// ===== AI 简历生成/重写 =====
const resumeGenLoading = ref(false);
const resumeGenMessage = ref("");
const resumeGenForm = reactive({
  targetRole: "",
  jdText: "",
  userBackground: ""
});
const generatedResume = reactive({
  mode: "",
  markdown: "",
  modelUsed: false,
  analysisId: null
});

// ===== 多轮优化对话 =====
const chatLoading = ref(false);
const chatMessage = ref("");
const chatState = reactive({
  sessionId: null,
  messages: [],
  input: ""
});

// ===== JD 雷达分析 =====
const radarLoading = ref(false);
const radarMessage = ref("");
const jdRadar = ref(null);

// ===== 简历真实性检测 =====
const auditLoading = ref(false);
const auditMessage = ref("");
const resumeAudit = ref(null);

// ===== AI 模拟面试 =====
const interviewLoading = ref(false);
const interviewMessage = ref("");
const interviewState = reactive({
  sessionId: null,
  targetRole: "",
  status: "ACTIVE",
  questionCount: 0,
  totalScore: null,
  messages: [],
  report: null,
  form: { targetRole: "", resumeText: "", jdText: "" },
  input: ""
});
const interviewSessions = ref([]);
const interviewSessionsPage = ref(0);
const interviewSessionsSize = ref(10);
const interviewSessionsTotal = ref(0);

// ===== 简历版本管理 =====
const versionsLoading = ref(false);
const versionsMessage = ref("");
const versionsState = reactive({
  items: [],
  page: 0,
  size: 20,
  total: 0,
  totalPages: 0,
  selectedVersion: null,
  compareMode: false,
  compareId1: null,
  compareId2: null,
  compareResult: null
});

// ===== 简历模板管理 =====
const templatesLoading = ref(false);
const templatesMessage = ref("");
const templatesState = reactive({
  items: [],
  page: 0,
  size: 20,
  total: 0,
  selectedTemplate: null,
  editingTemplate: null
});

// ===== 用户统计数据 =====
const userStatsLoading = ref(false);
const userStatsMessage = ref("");
const userStatsState = reactive({
  totalAnalyses: 0,
  avgScore: 0,
  scoreHistory: [],
  topMatchedKeywords: [],
  topMissingKeywords: []
});

// ===== 管理员统计数据 =====
const adminStatsLoading = ref(false);
const adminStatsMessage = ref("");
const adminStatsState = reactive({
  totalUsers: 0,
  activeUsers: 0,
  totalAnalyses: 0,
  avgScore: 0,
  dailyAnalysisCounts: [],
  popularRoles: []
});

// ===== 审计日志 =====
const auditLogsLoading = ref(false);
const auditLogsMessage = ref("");
const auditLogsState = reactive({
  items: [],
  page: 0,
  size: 20,
  total: 0,
  filters: { action: "", adminUsername: "", from: "", to: "" }
});

// ===== 岗位推荐 =====
const recommendLoading = ref(false);
const recommendMessage = ref("");
const recommendState = reactive({
  resumeText: "",
  recommendations: [],
  loading: false
});

// ===== 批量分析 =====
const batchLoading = ref(false);
const batchMessage = ref("");
const batchState = reactive({
  batchId: null,
  files: [],
  jdText: "",
  targetRole: "",
  status: null,
  jobs: []
});

// ===== 隐私脱敏 =====
const redactionLoading = ref(false);
const redactionMessage = ref("");
const redactionState = reactive({
  originalText: "",
  redactedText: "",
  redactedCount: 0,
  redactedItems: []
});

const queueJob = reactive({        // 当前排队任务状态
  jobId: "",                       // 任务 ID
  status: "",                      // 任务状态（PENDING/PROCESSING/DONE/FAILED）
  queuePosition: null,             // 排队位置
  vipPriority: false,              // 是否 VIP 优先
  errorMessage: "",                // 错误信息
  createdAt: "",                   // 创建时间
  startedAt: "",                   // 开始处理时间
  finishedAt: ""                   // 完成时间
});

let pollTimer = null;              // 轮询定时器引用
let pollDelayMs = 1200;            // 当前轮询间隔（自适应）

// ===== 我的记录 =====
const mineItems = ref([]);         // 我的分析记录列表
const mineMessage = ref("");       // 我的记录提示消息
const mineLocalKeyword = ref("");
const minePage = ref(0);
const mineSize = ref(10);
const mineTotal = ref(0);

// ===== 管理端：用户管理 =====
const adminUsers = ref([]);        // 用户列表
const adminUserQuery = reactive({ keyword: "" }); // 用户搜索关键词
const adminUsersMessage = ref(""); // 用户管理提示消息
const adminUserLoadingId = ref(null); // 正在操作的用户 ID（防重复点击）
const adminUserPage = ref(0);
const adminUserSize = ref(10);
const adminUserTotal = ref(0);

// ===== 管理端：模型配置 =====
const configLoading = ref(false);  // 配置保存加载状态
const configMessage = ref("");     // 配置提示消息
const configUpdatedAt = ref("");   // 配置最近更新时间
const configForm = reactive({      // 模型配置表单
  baseUrl: "",                     // AI 接口地址
  apiKey: "",                      // API 密钥
  model: ""                        // 模型名称
});

// ===== 管理端：客户简历 =====
const adminQuery = reactive({ username: "" }); // 客户简历搜索条件
const adminItems = ref([]);        // 客户简历列表
const adminMessage = ref("");      // 客户简历提示消息
const adminPage = ref(0);
const adminSize = ref(10);
const adminTotal = ref(0);

// ===== 管理端：面试题库 =====
const kbDocs = ref([]);            // 题库文档列表
const kbMessage = ref("");         // 题库操作提示消息
const kbLocalKeyword = ref("");
const kbDocPage = ref(0);
const kbDocSize = ref(10);
const kbDocTotal = ref(0);
const kbUploadLoading = ref(false);  // 文档上传加载状态
const kbCrawlLoading = ref(false);   // 爬虫入库加载状态
const kbLlmLoading = ref(false);     // 大模型生成加载状态
const kbCrawlErrors = ref([]);       // 爬虫失败页面列表
const kbUploadForm = reactive({      // 文档上传表单
  title: "",
  file: null
});
const kbCrawlForm = reactive({       // 爬虫入库表单
  seedUrl: "",                       // 种子链接
  title: "",                         // 文档标题
  topic: "",                         // 主题标签
  maxPages: 20,                      // 最大爬取页数
  maxDepth: 1,                       // 最大爬取深度
  sameDomainOnly: true,              // 是否仅抓取同域链接
  authCookie: "",                    // 私有站点 Cookie
  referer: "",                       // Referer 头
  authHeader: ""                     // Authorization 头
});
const kbLlmForm = reactive({         // 大模型生成表单
  topic: "",                         // 知识主题
  title: "",                         // 文档标题
  baseUrl: "",                       // 模型接口地址
  apiKey: "",                        // 模型 API Key
  model: "",                         // 模型名称
  questionCount: 30                  // 生成题目数量
});
const kbViewLoading = ref(false);    // 题库预览加载状态
const kbViewMessage = ref("");       // 题库预览提示消息
const kbViewer = reactive({          // 题库文档预览器状态
  visible: false,                    // 是否显示预览
  docId: null,                       // 当前预览的文档 ID
  title: "",                         // 文档标题
  filename: "",                      // 文件名
  total: 0,                          // 题目总数
  page: 0,                           // 当前页码
  size: 100,                         // 每页条数
  questions: []                      // 已加载的题目列表
});

// 计算属性：当前用户是否为管理员
const isAdmin = computed(() => (me.role || "").toUpperCase() === "ADMIN");
// 计算属性：根据认证模式返回对应的表单对象
const activeAuthForm = computed(() => (authMode.value === "login" ? loginForm : registerForm));
// 计算属性：题库预览是否还有更多数据可加载
const kbViewerHasMore = computed(() => kbViewer.questions.length < kbViewer.total);

const authHighlights = [
  {
    tag: "Workflow",
    title: "分析闭环",
    desc: "从简历上传、结果汇总到生成优化稿，关键动作保持在同一条主路径内。"
  },
  {
    tag: "Collaboration",
    title: "角色分层",
    desc: "普通用户专注分析体验，管理员聚焦用户、模型配置和知识库治理。"
  },
  {
    tag: "Feedback",
    title: "即时反馈",
    desc: "队列状态、结果摘要和关键指标在主工作区持续可见，减少跳转成本。"
  }
];

const analyzeTabItems = [
  { key: "analysis", label: "基础分析", desc: "提交简历并获得结构化结果" },
  { key: "generate", label: "生成 / 重写", desc: "按 JD 或分析结果生成简历" },
  { key: "chat", label: "优化对话", desc: "围绕结果做多轮追问与润色" },
  { key: "radar", label: "JD 雷达图", desc: "查看岗位维度匹配度分布" },
  { key: "audit", label: "真实性检测", desc: "识别夸大和风险表达" },
  { key: "interview", label: "AI 模拟面试", desc: "基于简历进行模拟面试" },
  { key: "recommend", label: "岗位推荐", desc: "根据简历推荐匹配岗位" },
  { key: "batch", label: "批量分析", desc: "批量处理多份简历" }
];

const workspaceNavItems = computed(() => {
  const items = [
    { key: "analyze", badge: "AI", label: "简历分析", desc: "提交任务、查看结果、继续优化" },
    { key: "mine", badge: "ME", label: "我的记录", desc: "回看历史分析与重点建议" },
    { key: "versions", badge: "VER", label: "版本管理", desc: "管理简历版本与对比" },
    { key: "stats", badge: "STA", label: "数据统计", desc: "查看个人分析统计数据" }
  ];
  if (isAdmin.value) {
    items.push(
      { key: "adminUsers", badge: "USR", label: "用户管理", desc: "统一维护账号、VIP 与黑名单" },
      { key: "adminConfig", badge: "CFG", label: "模型配置", desc: "配置模型地址、密钥与名称" },
      { key: "adminData", badge: "DB", label: "客户简历", desc: "检索履历数据与模型使用情况" },
      { key: "adminKb", badge: "KB", label: "面试题库", desc: "上传、抓取或生成知识资产" },
      { key: "adminAudit", badge: "LOG", label: "操作日志", desc: "查看管理员操作审计日志" },
      { key: "adminStats", badge: "SYS", label: "系统统计", desc: "查看系统整体统计数据" },
      { key: "adminTemplates", badge: "TPL", label: "模板管理", desc: "管理简历模板库" }
    );
  }
  return items;
});

const currentTabMeta = computed(() => {
  const map = {
    analyze: {
      kicker: "Core Workflow",
      title: "简历分析工作台",
      description: "围绕岗位匹配、AI 生成与后续优化，形成一条清晰的分析主链路。",
      tag: "分析闭环"
    },
    mine: {
      kicker: "History",
      title: "我的记录",
      description: "按时间回看历史分析结果，快速提炼最有价值的修改建议。",
      tag: "个人资产"
    },
    versions: {
      kicker: "Version Control",
      title: "版本管理",
      description: "保存、对比不同版本的简历，追踪优化历程。",
      tag: "版本追踪"
    },
    stats: {
      kicker: "Analytics",
      title: "数据统计",
      description: "查看个人分析历史、评分趋势与关键词匹配情况。",
      tag: "数据洞察"
    },
    adminUsers: {
      kicker: "Administration",
      title: "用户管理",
      description: "集中管理账号状态、VIP 权限与访问风险，减少后台操作成本。",
      tag: "账号治理"
    },
    adminConfig: {
      kicker: "Model Ops",
      title: "模型配置",
      description: "统一管理模型连接参数，让生成、题库与分析能力保持一致。",
      tag: "模型中台"
    },
    adminData: {
      kicker: "Data Review",
      title: "客户简历",
      description: "从用户维度查看简历分析数据与模型使用情况，便于运营回溯。",
      tag: "履历洞察"
    },
    adminKb: {
      kicker: "Knowledge Base",
      title: "面试题库",
      description: "通过上传、抓取与模型生成三种入口构建长期可复用的题库资产。",
      tag: "知识资产"
    },
    adminAudit: {
      kicker: "Audit Trail",
      title: "操作日志",
      description: "追踪管理员操作记录，确保系统安全与合规。",
      tag: "审计追踪"
    },
    adminStats: {
      kicker: "System Analytics",
      title: "系统统计",
      description: "查看系统整体运行数据、用户活跃度与分析趋势。",
      tag: "系统洞察"
    },
    adminTemplates: {
      kicker: "Template Library",
      title: "模板管理",
      description: "创建、编辑和管理简历模板库，提升用户体验。",
      tag: "模板库"
    }
  };
  return map[tab.value] || map.analyze;
});

const currentAnalyzeMeta = computed(() => analyzeTabItems.find((item) => item.key === analyzeSubtab.value) || analyzeTabItems[0]);

const sidebarInsights = computed(() => [
  { label: "当前身份", value: roleText(me.role) || "-" },
  { label: "任务优先级", value: me.vip ? "VIP" : "标准" },
  { label: "当前任务", value: queueJob.jobId ? statusText(queueJob.status) : "待提交" }
]);

const overviewStats = computed(() => {
  if (tab.value === "mine") {
    const latest = mineItems.value[0];
    return [
      { label: "当前加载", value: `${mineItems.value.length} 条`, desc: "展示最近一次拉取到的历史记录。" },
      { label: "最近岗位", value: latest?.targetRole || "-", desc: "用于快速定位最近分析的目标岗位。" },
      { label: "最近评分", value: latest ? formatScore(latest.score) : "-", desc: "帮助判断最近一次优化成效。" },
      { label: "最近时间", value: latest ? formatCompactTime(latest.createdAt) : "-", desc: "按时间倒序查看历史变化。" }
    ];
  }

  if (tab.value === "adminUsers") {
    const vipCount = adminUsers.value.filter((item) => item.vip).length;
    const blacklistedCount = adminUsers.value.filter((item) => item.blacklisted).length;
    return [
      { label: "当前加载", value: `${adminUsers.value.length} 人`, desc: "当前列表内的用户数量。" },
      { label: "VIP 用户", value: `${vipCount} 人`, desc: "用于观察高优先级账号规模。" },
      { label: "拉黑用户", value: `${blacklistedCount} 人`, desc: "快速掌握风险账号数量。" },
      { label: "当前筛选", value: adminUserQuery.keyword || "全部用户", desc: "支持按用户名做精确查询。" }
    ];
  }

  if (tab.value === "adminConfig") {
    return [
      { label: "接口地址", value: configForm.baseUrl ? "已配置" : "未配置", desc: "模型请求的统一入口。" },
      { label: "API Key", value: configForm.apiKey ? "已配置" : "未配置", desc: "敏感信息仍然通过后台统一维护。" },
      { label: "当前模型", value: configForm.model || "-", desc: "用于简历生成与题库能力。" },
      { label: "最近更新", value: formatCompactTime(configUpdatedAt.value), desc: "便于确认配置生效时间。" }
    ];
  }

  if (tab.value === "adminData") {
    const modelUsedCount = adminItems.value.filter((item) => item.modelUsed).length;
    return [
      { label: "当前加载", value: `${adminItems.value.length} 条`, desc: "当前页面已载入的客户简历记录。" },
      { label: "平均评分", value: adminItems.value.length ? formatScore(averageScore(adminItems.value, "score")) : "-", desc: "基于当前列表的评分均值。" },
      { label: "模型启用", value: `${modelUsedCount} 条`, desc: "显示模型参与分析的记录数量。" },
      { label: "用户筛选", value: adminQuery.username || "全部用户", desc: "支持按用户名查看客户履历。" }
    ];
  }

  if (tab.value === "adminKb") {
    return [
      { label: "文档数量", value: `${kbDocs.value.length} 份`, desc: "当前列表已加载的题库文档数量。" },
      { label: "预览状态", value: kbViewer.visible ? "预览中" : "未打开", desc: "便于快速判断当前查看动作。" },
      { label: "失败页面", value: `${kbCrawlErrors.value.length} 个`, desc: "爬取失败页面数会在这里汇总。" },
      { label: "入库入口", value: "上传 / 爬取 / 生成", desc: "三种方式共用同一知识资产区域。" }
    ];
  }

  if (tab.value === "versions") {
    return [
      { label: "版本总数", value: `${versionsState.total} 个`, desc: "已保存的简历版本总数。" },
      { label: "当前加载", value: `${versionsState.items.length} 个`, desc: "当前页面已加载的版本数量。" },
      { label: "对比模式", value: versionsState.compareMode ? "已开启" : "未开启", desc: "版本对比功能状态。" },
      { label: "选中版本", value: versionsState.selectedVersion ? `#${versionsState.selectedVersion.id}` : "-", desc: "当前查看的版本。" }
    ];
  }

  if (tab.value === "stats") {
    return [
      { label: "总分析数", value: `${userStatsState.totalAnalyses} 次`, desc: "累计分析次数。" },
      { label: "平均评分", value: formatScore(userStatsState.avgScore), desc: "所有分析的平均得分。" },
      { label: "高频关键词", value: `${userStatsState.topMatchedKeywords.length} 个`, desc: "最常匹配的关键词数量。" },
      { label: "缺失关键词", value: `${userStatsState.topMissingKeywords.length} 个`, desc: "最常缺失的关键词数量。" }
    ];
  }

  if (tab.value === "adminAudit") {
    return [
      { label: "日志总数", value: `${auditLogsState.total} 条`, desc: "审计日志总记录数。" },
      { label: "当前加载", value: `${auditLogsState.items.length} 条`, desc: "当前页面已加载的日志数量。" },
      { label: "操作筛选", value: auditLogsState.filters.action || "全部操作", desc: "当前筛选的操作类型。" },
      { label: "管理员筛选", value: auditLogsState.filters.adminUsername || "全部管理员", desc: "当前筛选的管理员。" }
    ];
  }

  if (tab.value === "adminStats") {
    return [
      { label: "总用户数", value: `${adminStatsState.totalUsers} 人`, desc: "系统注册用户总数。" },
      { label: "活跃用户", value: `${adminStatsState.activeUsers} 人`, desc: "近期活跃用户数量。" },
      { label: "总分析数", value: `${adminStatsState.totalAnalyses} 次`, desc: "系统累计分析次数。" },
      { label: "平均评分", value: formatScore(adminStatsState.avgScore), desc: "系统整体平均得分。" }
    ];
  }

  if (tab.value === "adminTemplates") {
    return [
      { label: "模板总数", value: `${templatesState.total} 个`, desc: "系统模板库总数。" },
      { label: "当前加载", value: `${templatesState.items.length} 个`, desc: "当前页面已加载的模板数量。" },
      { label: "选中模板", value: templatesState.selectedTemplate ? templatesState.selectedTemplate.name : "-", desc: "当前查看的模板。" },
      { label: "编辑状态", value: templatesState.editingTemplate ? "编辑中" : "未编辑", desc: "模板编辑状态。" }
    ];
  }

  return [
    { label: "当前模式", value: currentAnalyzeMeta.value.label, desc: "按步骤完成分析、生成、对话和检测。" },
    { label: "任务状态", value: queueJob.jobId ? statusText(queueJob.status) : "待提交", desc: "队列与分析状态持续反馈在主屏上。" },
    { label: "当前结果", value: result.value?.analysisId ? `#${result.value.analysisId}` : "暂无结果", desc: "完成分析后即可进入后续工具链。" },
    { label: "最新评分", value: result.value ? formatScore(result.value.score) : "-", desc: "用于快速判断当前岗位匹配度。" }
  ];
});

const filteredMineItems = computed(() => {
  const keyword = mineLocalKeyword.value.trim().toLowerCase();
  if (!keyword) return mineItems.value;
  return mineItems.value.filter((item) => {
    const fields = [item.filename, item.targetRole, item.optimizedSummary]
      .map((value) => String(value || "").toLowerCase());
    return fields.some((value) => value.includes(keyword));
  });
});

const mineStatItems = computed(() => {
  const latest = mineItems.value[0];
  return [
    { label: "记录数", value: `${mineTotal.value} 条` },
    { label: "平均评分", value: mineItems.value.length ? formatScore(averageScore(mineItems.value, "score")) : "-" },
    { label: "平均覆盖率", value: mineItems.value.length ? formatCoverage(averageScore(mineItems.value, "coverage")) : "-" },
    { label: "最近岗位", value: latest?.targetRole || "-" }
  ];
});

const adminUserStatItems = computed(() => {
  const vipCount = adminUsers.value.filter((item) => item.vip).length;
  const blacklistedCount = adminUsers.value.filter((item) => item.blacklisted).length;
  const adminCount = adminUsers.value.filter((item) => (item.role || "").toUpperCase() === "ADMIN").length;
  return [
    { label: "当前用户", value: `${adminUserTotal.value} 人` },
    { label: "VIP 用户", value: `${vipCount} 人` },
    { label: "管理员", value: `${adminCount} 人` },
    { label: "黑名单", value: `${blacklistedCount} 人` }
  ];
});

const filteredKbDocs = computed(() => {
  const keyword = kbLocalKeyword.value.trim().toLowerCase();
  if (!keyword) return kbDocs.value;
  return kbDocs.value.filter((item) => {
    const fields = [item.title, item.filename, item.uploadedBy]
      .map((value) => String(value || "").toLowerCase());
    return fields.some((value) => value.includes(keyword));
  });
});

const kbStatItems = computed(() => [
  { label: "文档总数", value: `${kbDocTotal.value} 份` },
  { label: "题目累计", value: `${sumField(kbDocs.value, "questionCount")} 题` },
  { label: "失败页面", value: `${kbCrawlErrors.value.length} 个` },
  { label: "当前预览", value: kbViewer.visible ? "已打开" : "未打开" }
]);

// ===== 分页 computed & helpers =====
const pageSizeOptions = [5, 10, 20, 50];
const mineTotalPages = computed(() => Math.ceil(mineTotal.value / mineSize.value) || 1);
const adminUserTotalPages = computed(() => Math.ceil(adminUserTotal.value / adminUserSize.value) || 1);
const adminTotalPages = computed(() => Math.ceil(adminTotal.value / adminSize.value) || 1);
const kbDocTotalPages = computed(() => Math.ceil(kbDocTotal.value / kbDocSize.value) || 1);

function pageRange(current, total) {
  const range = [];
  const delta = 2;
  let start = Math.max(0, current - delta);
  let end = Math.min(total - 1, current + delta);
  if (end - start < delta * 2) {
    start = Math.max(0, end - delta * 2);
    end = Math.min(total - 1, start + delta * 2);
  }
  for (let i = start; i <= end; i++) range.push(i);
  return range;
}

function mineGoPage(p) { minePage.value = p; loadMineAnalyses(); }
function mineChangeSize(s) { mineSize.value = s; minePage.value = 0; loadMineAnalyses(); }
function adminUserGoPage(p) { adminUserPage.value = p; loadAdminUsers(); }
function adminUserChangeSize(s) { adminUserSize.value = s; adminUserPage.value = 0; loadAdminUsers(); }
function adminGoPage(p) { adminPage.value = p; loadAdminAnalyses(); }
function adminChangeSize(s) { adminSize.value = s; adminPage.value = 0; loadAdminAnalyses(); }
function kbDocGoPage(p) { kbDocPage.value = p; loadKbDocs(); }
function kbDocChangeSize(s) { kbDocSize.value = s; kbDocPage.value = 0; loadKbDocs(); }

// 统一注入鉴权头，避免每个请求重复拼 Authorization
function authHeaders(extra = {}) {
  const headers = { ...extra };
  if (token.value) {
    headers.Authorization = `Bearer ${token.value}`;
  }
  return headers;
}

// 将后端英文错误消息映射为中文提示
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
    "target_role is required": "请填写目标岗位",
    "analysis_id is required": "缺少分析记录ID",
    "analysis record not found": "分析记录不存在",
    "cannot access this analysis record": "无权访问该分析记录",
    "message is required": "请输入对话内容",
    "chat session not found": "对话会话不存在",
    "cannot access this chat session": "无权访问该对话会话",
    "jd_text is required": "请填写JD文本",
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

// 将角色标识转为中文显示文本
function roleText(role) {
  return (role || "").toUpperCase() === "ADMIN" ? "管理员" : "普通用户";
}

// 布尔值转中文"是/否"
function booleanText(v) {
  return v ? "是" : "否";
}

// 安全转数字，非法值返回 0
function asNumber(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

// 格式化评分为两位小数
// 格式化评分为两位小数
function formatScore(value) {
  return asNumber(value).toFixed(2);
}

// 根据评分返回对应的 CSS 等级类名（高/中/低）
function scoreLevelClass(value) {
  const n = asNumber(value);
  if (n >= 75) return "score-high";
  if (n >= 55) return "score-mid";
  return "score-low";
}

// 将覆盖率转为百分比字符串（用于进度条宽度）
// 将覆盖率转为百分比字符串（用于进度条宽度）
function coveragePercent(value) {
  const n = Math.max(0, Math.min(100, asNumber(value)));
  return `${n}%`;
}

// 格式化覆盖率为百分比显示文本
function formatCoverage(value) {
  const n = Math.max(0, Math.min(100, asNumber(value)));
  const rounded = Math.round(n * 100) / 100;
  return Number.isInteger(rounded) ? `${rounded}%` : `${rounded.toFixed(2)}%`;
}

function averageScore(items, field) {
  const rows = Array.isArray(items) ? items : [];
  if (!rows.length) return 0;
  const total = rows.reduce((sum, item) => sum + asNumber(item?.[field]), 0);
  return total / rows.length;
}

function sumField(items, field) {
  const rows = Array.isArray(items) ? items : [];
  return rows.reduce((sum, item) => sum + asNumber(item?.[field]), 0);
}

// 将任务状态码转为中文文本
function statusText(status) {
  const map = {
    PENDING: "排队中",
    PROCESSING: "分析中",
    DONE: "已完成",
    FAILED: "失败"
  };
  return map[status] || status || "-";
}

// 根据任务状态返回对应的 CSS 类名（用于颜色区分）
function statusClass(status) {
  const v = String(status || "").toUpperCase();
  if (v === "DONE") return "status-done";
  if (v === "FAILED") return "status-failed";
  if (v === "PROCESSING") return "status-processing";
  return "status-pending";
}

// 根据标签页 key 返回中文标题
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

async function openWorkspaceTab(tabKey) {
  if (tabKey === "mine") {
    await switchMine();
    return;
  }
  if (tabKey === "adminUsers") {
    await switchAdminUsers();
    return;
  }
  if (tabKey === "adminConfig") {
    await switchAdminConfig();
    return;
  }
  if (tabKey === "adminData") {
    await switchAdminData();
    return;
  }
  if (tabKey === "adminKb") {
    await switchAdminKb();
    return;
  }
  tab.value = "analyze";
}

async function copyText(text) {
  const value = String(text || "").trim();
  if (!value) return;
  try {
    await navigator.clipboard.writeText(value);
  } catch {
    // ignore
  }
}

function downloadText(filename, text) {
  const value = String(text || "");
  if (!value) return;
  const blob = new Blob([value], { type: "text/markdown;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

// 通用 API 请求封装：自动注入鉴权头、解析 JSON、统一错误处理
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

// token 持久化策略: 勾选“记住我”写入 localStorage，否则写 sessionStorage
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

// 切换认证模式（登录 <-> 注册）
function setAuthMode(mode) {
  authMode.value = mode;
  authMessage.value = "";
}

// 切换登录/注册模式
// 快捷切换登录/注册模式
function toggleAuthMode() {
  setAuthMode(authMode.value === "login" ? "register" : "login");
}

// 提交登录请求，成功后保存 token 并加载用户信息
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

// 提交注册请求，成功后自动登录
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

// 统一认证提交入口：根据当前模式调用登录或注册
// 统一认证提交入口：根据当前模式调用登录或注册
async function submitAuth() {
  if (authMode.value === "login") {
    await submitLogin();
  } else {
    await submitRegister();
  }
}

// 停止轮询器，避免重复创建定时器或页面离开后继续请求
function stopPolling() {
  if (pollTimer) {
    clearTimeout(pollTimer);
    pollTimer = null;
  }
}

function scheduleNextPoll(jobId) {
  stopPolling();
  pollTimer = setTimeout(() => {
    pollJobStatus(jobId);
  }, pollDelayMs);
}

// 重置排队任务状态
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

// 读取当前登录用户信息，用于渲染权限与角色视图
async function loadMe() {
  if (!token.value) return;
  const data = await apiRequest("/auth/me");
  me.id = data.id;
  me.username = data.username;
  me.role = data.role;
  me.vip = !!data.vip;
  me.blacklisted = !!data.blacklisted;
}

// 退出时清空所有状态，避免下一位用户看到上一个会话残留数据
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
  resumeGenMessage.value = "";
  resumeGenLoading.value = false;
  resumeGenForm.targetRole = "";
  resumeGenForm.jdText = "";
  resumeGenForm.userBackground = "";
  generatedResume.mode = "";
  generatedResume.markdown = "";
  generatedResume.modelUsed = false;
  generatedResume.analysisId = null;
  chatLoading.value = false;
  chatMessage.value = "";
  chatState.sessionId = null;
  chatState.messages = [];
  chatState.input = "";
  radarLoading.value = false;
  radarMessage.value = "";
  jdRadar.value = null;
  auditLoading.value = false;
  auditMessage.value = "";
  resumeAudit.value = null;
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

// 简历文件选择回调
function onFileChange(e) {
  const files = e.target.files || [];
  analyzeForm.file = files[0] || null;
}

// JD 图片文件选择回调
function onJdImageChange(e) {
  const files = e.target.files || [];
  analyzeForm.jdImage = files[0] || null;
}

// 题库文档文件选择回调
function onKbFileChange(e) {
  const files = e.target.files || [];
  kbUploadForm.file = files[0] || null;
}

// 轮询任务状态：根据状态更新 UI，完成或失败时停止轮询
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
      // pending 阶段逐步放大间隔，降低服务器轮询压力
      pollDelayMs = Math.min(5000, pollDelayMs + 400);
      scheduleNextPoll(jobId);
      return;
    }

    if (queueJob.status === "PROCESSING") {
      analyzeMessage.value = "任务正在分析中，请稍候...";
      // processing 阶段保持较快刷新，提升完成感知速度
      pollDelayMs = 1400;
      scheduleNextPoll(jobId);
      return;
    }

    if (queueJob.status === "DONE") {
      stopPolling();
      result.value = statusData.result || null;
      analyzeSubtab.value = "analysis";
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

// 启动轮询：先立即拉一次，再按固定间隔刷新
function startPolling(jobId) {
  stopPolling();
  pollDelayMs = 1200;
  pollJobStatus(jobId);
}

// 手动刷新当前任务状态
// 手动刷新当前任务状态
async function refreshCurrentJob() {
  if (!queueJob.jobId) return;
  await pollJobStatus(queueJob.jobId);
}

// 提交分析任务（multipart），并进入排队状态轮询
async function submitAnalyze() {
  if (!analyzeForm.file) {
    analyzeMessage.value = "请先选择简历文件";
    return;
  }
  analyzeLoading.value = true;
  analyzeMessage.value = "";
  result.value = null;
  chatState.sessionId = null;
  chatState.messages = [];
  chatState.input = "";
  chatMessage.value = "";
  jdRadar.value = null;
  radarMessage.value = "";
  resumeAudit.value = null;
  auditMessage.value = "";
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

async function generateResumeFromJd() {
  if (!resumeGenForm.targetRole || !resumeGenForm.jdText) {
    resumeGenMessage.value = "请填写目标岗位和 JD";
    return;
  }
  resumeGenLoading.value = true;
  resumeGenMessage.value = "";
  try {
    const data = await apiRequest("/resume/generate", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        targetRole: resumeGenForm.targetRole,
        jdText: resumeGenForm.jdText,
        userBackground: resumeGenForm.userBackground
      })
    });
    generatedResume.mode = data.mode || "";
    generatedResume.markdown = data.markdown || "";
    generatedResume.modelUsed = !!data.modelUsed;
    generatedResume.analysisId = data.analysisId ?? null;
    resumeGenMessage.value = generatedResume.modelUsed
      ? "生成成功（AI模型）"
      : "生成完成（本地模板兜底）";
  } catch (err) {
    resumeGenMessage.value = toZhMessage(err.message || "简历生成失败");
  } finally {
    resumeGenLoading.value = false;
  }
}

async function rewriteResumeFromCurrentAnalysis() {
  const analysisId = result.value?.analysisId;
  if (!analysisId) {
    resumeGenMessage.value = "当前没有可重写的分析记录";
    return;
  }
  resumeGenLoading.value = true;
  resumeGenMessage.value = "";
  try {
    const data = await apiRequest("/resume/rewrite", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ analysisId })
    });
    generatedResume.mode = data.mode || "";
    generatedResume.markdown = data.markdown || "";
    generatedResume.modelUsed = !!data.modelUsed;
    generatedResume.analysisId = data.analysisId ?? null;
    resumeGenMessage.value = generatedResume.modelUsed
      ? "重写成功（AI模型）"
      : "重写完成（本地模板兜底）";
  } catch (err) {
    resumeGenMessage.value = toZhMessage(err.message || "重写失败");
  } finally {
    resumeGenLoading.value = false;
  }
}

async function copyGeneratedResume() {
  await copyText(generatedResume.markdown);
  resumeGenMessage.value = "已复制到剪贴板";
}

function downloadGeneratedResume() {
  downloadText("generated-resume.md", generatedResume.markdown);
}

async function startResumeChat() {
  const analysisId = result.value?.analysisId;
  if (!analysisId) {
    chatMessage.value = "请先完成一次分析";
    return;
  }
  chatLoading.value = true;
  chatMessage.value = "";
  try {
    const data = await apiRequest("/chat/start", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ analysisId })
    });
    chatState.sessionId = data.sessionId;
    chatState.messages = data.messages || [];
    chatState.input = "";
    chatMessage.value = `会话已创建（ID: ${data.sessionId}）`;
  } catch (err) {
    chatMessage.value = toZhMessage(err.message || "开启会话失败");
  } finally {
    chatLoading.value = false;
  }
}

async function sendChatMessage() {
  if (!chatState.sessionId) {
    chatMessage.value = "请先开启会话";
    return;
  }
  if (!chatState.input) return;

  const ask = chatState.input;
  chatLoading.value = true;
  chatMessage.value = "";
  try {
    const data = await apiRequest(`/chat/${chatState.sessionId}/message`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message: ask })
    });
    chatState.messages = data.messages || [];
    chatState.input = "";
  } catch (err) {
    chatMessage.value = toZhMessage(err.message || "发送失败");
  } finally {
    chatLoading.value = false;
  }
}

async function quickAsk(text) {
  chatState.input = text;
  await sendChatMessage();
}

async function runJdRadarFromCurrent() {
  const analysisId = result.value?.analysisId;
  if (!analysisId) {
    radarMessage.value = "请先完成一次分析";
    return;
  }
  radarLoading.value = true;
  radarMessage.value = "";
  try {
    const data = await apiRequest("/jd/analyze", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ analysisId })
    });
    jdRadar.value = data;
    radarMessage.value = "雷达图数据生成成功";
  } catch (err) {
    radarMessage.value = toZhMessage(err.message || "JD 解析失败");
  } finally {
    radarLoading.value = false;
  }
}

async function runAuditFromCurrent() {
  const analysisId = result.value?.analysisId;
  if (!analysisId) {
    auditMessage.value = "请先完成一次分析";
    return;
  }
  auditLoading.value = true;
  auditMessage.value = "";
  try {
    const data = await apiRequest("/resume/audit", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ analysisId, targetRole: analyzeForm.targetRole || "" })
    });
    resumeAudit.value = data;
    auditMessage.value = "真实性检测完成";
  } catch (err) {
    auditMessage.value = toZhMessage(err.message || "真实性检测失败");
  } finally {
    auditLoading.value = false;
  }
}

// ===== 业务列表加载函数 =====
// 加载我的分析记录列表（分页）
async function loadMineAnalyses() {
  try {
    const data = await apiRequest(`/analyses/mine?page=${minePage.value}&size=${mineSize.value}`);
    mineItems.value = data.items || [];
    mineTotal.value = data.total ?? 0;
    mineMessage.value = "";
  } catch (err) {
    mineMessage.value = toZhMessage(err.message || "加载失败");
  }
}

// 加载管理员用户列表（支持关键词搜索）
async function loadAdminUsers() {
  try {
    const keyword = encodeURIComponent(adminUserQuery.keyword || "");
    const data = await apiRequest(`/admin/users?page=${adminUserPage.value}&size=${adminUserSize.value}&keyword=${keyword}`);
    adminUsers.value = data.items || [];
    adminUserTotal.value = data.total ?? 0;
    adminUsersMessage.value = "";
  } catch (err) {
    adminUsersMessage.value = toZhMessage(err.message || "加载用户失败");
  }
}

// 更新用户标记（VIP/拉黑），更新后刷新列表
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

// 切换用户 VIP 状态
// 切换用户 VIP 状态
async function toggleVip(user) {
  await updateUserFlags(user.id, { vip: !user.vip });
}

// 切换用户拉黑状态
async function toggleBlacklist(user) {
  await updateUserFlags(user.id, { blacklisted: !user.blacklisted });
}

// 加载当前模型配置（管理员）
// 加载当前模型配置（管理员）
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

// 题库模型导入默认值读取自管理员模型配置，减少重复输入
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

// 保存模型配置到后端
// 保存模型配置到后端
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

// 加载管理员客户简历数据（支持按用户名搜索）
// 加载管理员客户简历数据（支持按用户名搜索）
async function loadAdminAnalyses() {
  try {
    const query = encodeURIComponent(adminQuery.username || "");
    const data = await apiRequest(`/admin/analyses?page=${adminPage.value}&size=${adminSize.value}&username=${query}`);
    adminItems.value = data.items || [];
    adminTotal.value = data.total ?? 0;
    adminMessage.value = "";
  } catch (err) {
    adminMessage.value = toZhMessage(err.message || "加载客户数据失败");
  }
}

// 加载面试题库文档列表
async function loadKbDocs() {
  try {
    const data = await apiRequest(`/admin/interview-kb/docs?page=${kbDocPage.value}&size=${kbDocSize.value}`);
    kbDocs.value = data.items || [];
    kbDocTotal.value = data.total ?? 0;
    kbMessage.value = "";
  } catch (err) {
    kbMessage.value = toZhMessage(err.message || "加载面试题库失败");
  }
}

// ===== 题库查看器 =====
// 打开题库文档预览器，初始化并加载第一页
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

// 关闭题库文档预览器并重置状态
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

// 加载题库文档内容（分页），reset=true 时从第一页开始
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

// 加载更多题库内容（下一页）
async function loadKbDocViewerMore() {
  if (!kbViewerHasMore.value) return;
  await loadKbDocViewer(false);
}

// 上传面试文档并入库
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

// 爬虫抓取面试题并入库
// 爬虫抓取面试题并入库
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

// 调用大模型生成面试题并入库
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

// 删除指定题库文档
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

// 切换到"我的记录"标签并加载数据
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

// ===== AI 模拟面试相关函数 =====
async function startInterview() {
  if (!interviewState.form.targetRole) {
    interviewMessage.value = "请填写目标岗位";
    return;
  }
  interviewLoading.value = true;
  interviewMessage.value = "";
  try {
    const payload = {
      targetRole: interviewState.form.targetRole,
      resumeText: interviewState.form.resumeText || "",
      jdText: interviewState.form.jdText || ""
    };
    const data = await apiRequest("/interview/start", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    interviewState.sessionId = data.sessionId;
    interviewState.targetRole = data.targetRole;
    interviewState.status = data.status;
    interviewState.questionCount = data.questionCount || 0;
    interviewState.totalScore = data.totalScore;
    interviewState.messages = [];
    await loadInterviewMessages(data.sessionId);
    interviewMessage.value = "面试已开始";
  } catch (err) {
    interviewMessage.value = toZhMessage(err.message || "开始面试失败");
  } finally {
    interviewLoading.value = false;
  }
}

async function answerQuestion() {
  if (!interviewState.input.trim()) {
    interviewMessage.value = "请输入回答内容";
    return;
  }
  interviewLoading.value = true;
  interviewMessage.value = "";
  try {
    const payload = { answer: interviewState.input };
    const data = await apiRequest(`/interview/${interviewState.sessionId}/answer`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    interviewState.input = "";
    await loadInterviewMessages(interviewState.sessionId);
    interviewState.questionCount = data.questionCount || interviewState.questionCount;
    interviewState.totalScore = data.totalScore;
  } catch (err) {
    interviewMessage.value = toZhMessage(err.message || "提交回答失败");
  } finally {
    interviewLoading.value = false;
  }
}

async function endInterview() {
  interviewLoading.value = true;
  interviewMessage.value = "";
  try {
    const data = await apiRequest(`/interview/${interviewState.sessionId}/end`, { method: "POST" });
    interviewState.status = data.status;
    interviewState.report = data.report;
    interviewMessage.value = "面试已结束";
  } catch (err) {
    interviewMessage.value = toZhMessage(err.message || "结束面试失败");
  } finally {
    interviewLoading.value = false;
  }
}

async function loadInterviewMessages(sessionId) {
  try {
    const data = await apiRequest(`/interview/${sessionId}/messages`);
    interviewState.messages = data.messages || [];
  } catch (err) {
    interviewMessage.value = toZhMessage(err.message || "加载消息失败");
  }
}

async function loadInterviewSessions(page = 0) {
  interviewLoading.value = true;
  try {
    const data = await apiRequest(`/interview/sessions?page=${page}&size=${interviewSessionsSize.value}`);
    interviewSessions.value = data.content || [];
    interviewSessionsPage.value = data.page || 0;
    interviewSessionsTotal.value = data.total || 0;
  } catch (err) {
    interviewMessage.value = toZhMessage(err.message || "加载面试记录失败");
  } finally {
    interviewLoading.value = false;
  }
}

// ===== 简历版本管理相关函数 =====
async function loadVersions(page = 0) {
  versionsLoading.value = true;
  versionsMessage.value = "";
  try {
    const data = await apiRequest(`/versions?page=${page}&size=${versionsState.size}`);
    versionsState.items = data.content || [];
    versionsState.page = data.page || 0;
    versionsState.total = data.total || 0;
    versionsState.totalPages = data.totalPages || 0;
  } catch (err) {
    versionsMessage.value = toZhMessage(err.message || "加载版本列表失败");
  } finally {
    versionsLoading.value = false;
  }
}

async function saveVersion(name, content) {
  versionsLoading.value = true;
  versionsMessage.value = "";
  try {
    const payload = { name, content };
    await apiRequest("/versions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    versionsMessage.value = "版本保存成功";
    await loadVersions(versionsState.page);
  } catch (err) {
    versionsMessage.value = toZhMessage(err.message || "保存版本失败");
  } finally {
    versionsLoading.value = false;
  }
}

async function deleteVersion(versionId) {
  versionsLoading.value = true;
  versionsMessage.value = "";
  try {
    await apiRequest(`/versions/${versionId}`, { method: "DELETE" });
    versionsMessage.value = "版本已删除";
    await loadVersions(versionsState.page);
  } catch (err) {
    versionsMessage.value = toZhMessage(err.message || "删除版本失败");
  } finally {
    versionsLoading.value = false;
  }
}

async function compareVersions(id1, id2) {
  versionsLoading.value = true;
  versionsMessage.value = "";
  try {
    const data = await apiRequest(`/versions/compare?id1=${id1}&id2=${id2}`);
    versionsState.compareResult = data;
    versionsMessage.value = "对比完成";
  } catch (err) {
    versionsMessage.value = toZhMessage(err.message || "版本对比失败");
  } finally {
    versionsLoading.value = false;
  }
}

// ===== 简历模板管理相关函数 =====
async function loadTemplates(page = 0) {
  templatesLoading.value = true;
  templatesMessage.value = "";
  try {
    const data = await apiRequest(`/admin/templates?page=${page}&size=${templatesState.size}`);
    templatesState.items = data.content || [];
    templatesState.page = data.page || 0;
    templatesState.total = data.total || 0;
  } catch (err) {
    templatesMessage.value = toZhMessage(err.message || "加载模板列表失败");
  } finally {
    templatesLoading.value = false;
  }
}

async function createTemplate(template) {
  templatesLoading.value = true;
  templatesMessage.value = "";
  try {
    await apiRequest("/admin/templates", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(template)
    });
    templatesMessage.value = "模板创建成功";
    await loadTemplates(templatesState.page);
  } catch (err) {
    templatesMessage.value = toZhMessage(err.message || "创建模板失败");
  } finally {
    templatesLoading.value = false;
  }
}

async function updateTemplate(templateId, template) {
  templatesLoading.value = true;
  templatesMessage.value = "";
  try {
    await apiRequest(`/admin/templates/${templateId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(template)
    });
    templatesMessage.value = "模板更新成功";
    await loadTemplates(templatesState.page);
  } catch (err) {
    templatesMessage.value = toZhMessage(err.message || "更新模板失败");
  } finally {
    templatesLoading.value = false;
  }
}

async function deleteTemplate(templateId) {
  templatesLoading.value = true;
  templatesMessage.value = "";
  try {
    await apiRequest(`/admin/templates/${templateId}`, { method: "DELETE" });
    templatesMessage.value = "模板已删除";
    await loadTemplates(templatesState.page);
  } catch (err) {
    templatesMessage.value = toZhMessage(err.message || "删除模板失败");
  } finally {
    templatesLoading.value = false;
  }
}

async function applyTemplate(templateId) {
  templatesLoading.value = true;
  templatesMessage.value = "";
  try {
    const data = await apiRequest(`/templates/${templateId}/apply`);
    templatesMessage.value = "模板应用成功";
    return data;
  } catch (err) {
    templatesMessage.value = toZhMessage(err.message || "应用模板失败");
    return null;
  } finally {
    templatesLoading.value = false;
  }
}

// ===== 统计数据相关函数 =====
async function loadUserStats() {
  userStatsLoading.value = true;
  userStatsMessage.value = "";
  try {
    const data = await apiRequest("/stats/user");
    Object.assign(userStatsState, data);
  } catch (err) {
    userStatsMessage.value = toZhMessage(err.message || "加载统计数据失败");
  } finally {
    userStatsLoading.value = false;
  }
}

async function loadAdminStats() {
  adminStatsLoading.value = true;
  adminStatsMessage.value = "";
  try {
    const data = await apiRequest("/admin/stats");
    Object.assign(adminStatsState, data);
  } catch (err) {
    adminStatsMessage.value = toZhMessage(err.message || "加载系统统计失败");
  } finally {
    adminStatsLoading.value = false;
  }
}

// ===== 审计日志相关函数 =====
async function loadAuditLogs(page = 0) {
  auditLogsLoading.value = true;
  auditLogsMessage.value = "";
  try {
    const params = new URLSearchParams({
      page: page.toString(),
      size: auditLogsState.size.toString()
    });
    if (auditLogsState.filters.action) params.append("action", auditLogsState.filters.action);
    if (auditLogsState.filters.adminUsername) params.append("adminUsername", auditLogsState.filters.adminUsername);
    if (auditLogsState.filters.from) params.append("from", auditLogsState.filters.from);
    if (auditLogsState.filters.to) params.append("to", auditLogsState.filters.to);

    const data = await apiRequest(`/admin/audit-logs?${params.toString()}`);
    auditLogsState.items = data.content || [];
    auditLogsState.page = data.page || 0;
    auditLogsState.total = data.total || 0;
  } catch (err) {
    auditLogsMessage.value = toZhMessage(err.message || "加载审计日志失败");
  } finally {
    auditLogsLoading.value = false;
  }
}

// ===== 岗位推荐相关函数 =====
async function recommendJobs() {
  if (!recommendState.resumeText.trim()) {
    recommendMessage.value = "请输入简历内容";
    return;
  }
  recommendLoading.value = true;
  recommendMessage.value = "";
  try {
    const payload = { resumeText: recommendState.resumeText };
    const data = await apiRequest("/recommend", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    recommendState.recommendations = data.recommendations || [];
    recommendMessage.value = `找到 ${recommendState.recommendations.length} 个推荐岗位`;
  } catch (err) {
    recommendMessage.value = toZhMessage(err.message || "岗位推荐失败");
  } finally {
    recommendLoading.value = false;
  }
}

// ===== 批量分析相关函数 =====
async function submitBatch() {
  if (!batchState.files.length) {
    batchMessage.value = "请选择至少一份简历文件";
    return;
  }
  batchLoading.value = true;
  batchMessage.value = "";
  try {
    const fd = new FormData();
    batchState.files.forEach((file) => fd.append("files", file));
    fd.append("jdText", batchState.jdText || "");
    fd.append("targetRole", batchState.targetRole || "");

    const data = await apiRequest("/analyze/batch", {
      method: "POST",
      headers: authHeaders(),
      body: fd
    });
    batchState.batchId = data.batchId;
    batchState.status = data.status;
    batchMessage.value = "批量任务已提交";
    await loadBatchStatus(data.batchId);
  } catch (err) {
    batchMessage.value = toZhMessage(err.message || "提交批量任务失败");
  } finally {
    batchLoading.value = false;
  }
}

async function loadBatchStatus(batchId) {
  try {
    const data = await apiRequest(`/analyze/batch/${batchId}`);
    batchState.status = data.status;
    batchState.jobs = data.jobs || [];
  } catch (err) {
    batchMessage.value = toZhMessage(err.message || "加载批量任务状态失败");
  }
}

// ===== 隐私脱敏相关函数 =====
async function redactPrivacy() {
  if (!redactionState.originalText.trim()) {
    redactionMessage.value = "请输入需要脱敏的文本";
    return;
  }
  redactionLoading.value = true;
  redactionMessage.value = "";
  try {
    const payload = { text: redactionState.originalText };
    const data = await apiRequest("/privacy/redact", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    redactionState.redactedText = data.redactedText;
    redactionState.redactedCount = data.redactedCount || 0;
    redactionState.redactedItems = data.redactedItems || [];
    redactionMessage.value = `已脱敏 ${redactionState.redactedCount} 处敏感信息`;
  } catch (err) {
    redactionMessage.value = toZhMessage(err.message || "隐私脱敏失败");
  } finally {
    redactionLoading.value = false;
  }
}

// ===== 标签页切换函数 =====
async function switchVersions() {
  tab.value = "versions";
  await loadVersions();
}

async function switchStats() {
  tab.value = "stats";
  await loadUserStats();
}

async function switchAdminAudit() {
  tab.value = "adminAudit";
  await loadAuditLogs();
}

async function switchAdminStats() {
  tab.value = "adminStats";
  await loadAdminStats();
}

async function switchAdminTemplates() {
  tab.value = "adminTemplates";
  await loadTemplates();
}

// ===== 辅助函数 =====
function toggleCompareVersion(versionId) {
  if (versionsState.compareId1 === versionId) {
    versionsState.compareId1 = null;
  } else if (versionsState.compareId2 === versionId) {
    versionsState.compareId2 = null;
  } else if (!versionsState.compareId1) {
    versionsState.compareId1 = versionId;
  } else if (!versionsState.compareId2) {
    versionsState.compareId2 = versionId;
  } else {
    versionsState.compareId1 = versionId;
    versionsState.compareId2 = null;
  }
}

async function saveTemplate() {
  if (!templatesState.editingTemplate.name || !templatesState.editingTemplate.content) {
    templatesMessage.value = "请填写模板名称和内容";
    return;
  }
  if (templatesState.editingTemplate.id) {
    await updateTemplate(templatesState.editingTemplate.id, templatesState.editingTemplate);
  } else {
    await createTemplate(templatesState.editingTemplate);
  }
  templatesState.editingTemplate = null;
}

// 后端返回 ISO 时间，本地统一格式化成浏览器可读时间
function formatTime(v) {
  if (!v) return "-";
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return v;
  return d.toLocaleString();
}

function formatCompactTime(v) {
  if (!v) return "-";
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return v;
  return d.toLocaleDateString();
}

// 首屏进入时，若有 token 则自动恢复会话
onMounted(async () => {
  if (!token.value) return;
  try {
    await loadMe();
  } catch {
    await logout();
  }
});

// 组件销毁前清理轮询器，避免内存泄漏与无效请求
onBeforeUnmount(() => {
  stopPolling();
});
</script>
