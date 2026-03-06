# AI 功能扩展实现方案

## 概述

在现有简历分析系统基础上，新增 5 个 AI 功能模块。前端按 Vue 组件拆分，后端新增 Service + DTO + API 端点。

## 前端架构调整

当前 App.vue 已 1470 行，新功能以独立 `.vue` 组件实现，通过 tab 切换加载：

```
src/
├── App.vue                    # 主框架（登录/布局/tab路由）
├── main.js
├── style.css
└── components/
    ├── MockInterview.vue      # 功能1：AI 模拟面试
    ├── ResumeChat.vue         # 功能2：多轮对话优化助手
    ├── ResumeGenerator.vue    # 功能3：简历一键生成/重写
    ├── JdRadar.vue            # 功能4：JD 智能解析 + 雷达图
    └── ResumeAudit.vue        # 功能5：简历真实性检测
```

App.vue 侧边栏新增 5 个 tab 按钮，主内容区用 `<component :is="...">` 动态加载。

## 功能1：AI 模拟面试

### 后端
- `MockInterviewService.java` — 核心服务
  - `startInterview(targetRole, resumeText, jdText, userId)` → 创建面试会话，从题库抽题 + LLM 生成开场
  - `answerQuestion(sessionId, userAnswer)` → 用户回答后，LLM 评分 + 追问/下一题
  - `endInterview(sessionId)` → 结束面试，LLM 生成总评报告
- `MockInterviewSession` (Model) — 面试会话表：id, userId, targetRole, status, totalScore, createdAt
- `MockInterviewMessage` (Model) — 对话消息表：id, sessionId, role(AI/USER), content, score, createdAt
- DTO: `StartInterviewRequest`, `InterviewAnswerRequest`, `InterviewMessageResponse`, `InterviewReportResponse`
- API: `POST /api/interview/start`, `POST /api/interview/{sessionId}/answer`, `POST /api/interview/{sessionId}/end`, `GET /api/interview/{sessionId}/messages`

### 前端 MockInterview.vue
- 聊天式 UI：左侧 AI 消息，右侧用户消息
- 顶部显示当前题号、累计得分
- 输入框 + 发送按钮
- 结束面试按钮 → 展示总评报告（得分、强项、弱项、建议）

### LLM Prompt 策略
- system: "你是资深技术面试官，针对{targetRole}岗位进行面试。根据候选人简历和JD逐题提问，每次只问一个问题。"
- 评分维度：技术深度(0-30)、表达清晰度(0-20)、实战经验(0-30)、思维逻辑(0-20)
- 每轮返回 JSON: `{ "score": 75, "feedback": "...", "nextQuestion": "..." }`

---

## 功能2：多轮对话优化助手

### 后端
- `ResumeChatService.java` — 核心服务
  - `chat(sessionId, userMessage, analysisId)` → 带上下文的多轮对话
  - 上下文包含：原始简历文本、JD、ATS 分析结果、历史对话
- `ChatSession` (Model) — 对话会话表：id, userId, analysisId, createdAt
- `ChatMessage` (Model) — 对话消息表：id, sessionId, role, content, createdAt
- DTO: `ChatRequest`, `ChatMessageResponse`
- API: `POST /api/chat/start`, `POST /api/chat/{sessionId}/message`, `GET /api/chat/{sessionId}/messages`

### 前端 ResumeChat.vue
- 从"我的记录"中选择一条分析结果，点击"AI 优化助手"进入
- 聊天界面，预置快捷问题："这条经历怎么改？"、"技能怎么分组？"、"缺失关键词怎么补？"
- 支持复制 AI 回复内容

### LLM Prompt 策略
- system: "你是简历优化专家。基于以下分析结果帮助用户逐条优化简历。回答要具体、可直接使用。"
- 每轮携带最近 10 条对话历史 + 分析结果摘要

---

## 功能3：简历一键生成/重写

### 后端
- `ResumeGeneratorService.java` — 核心服务
  - `generate(targetRole, jdText, userBackground)` → 从零生成简历
  - `rewrite(analysisId)` → 基于已有分析结果重写简历
  - 返回 Markdown 格式的完整简历
- DTO: `GenerateResumeRequest`, `RewriteResumeRequest`, `GeneratedResumeResponse`
- API: `POST /api/resume/generate`, `POST /api/resume/rewrite`

### 前端 ResumeGenerator.vue
- 两个模式切换：
  - "从零生成"：填写目标岗位、JD、个人背景（教育、技能、经历关键词）
  - "智能重写"：选择历史分析记录，一键重写
- 结果区：Markdown 渲染 + 复制按钮 + 导出纯文本按钮

### LLM Prompt 策略
- 生成模式 system: "你是专业简历撰写师。根据目标岗位和JD生成一份高ATS评分的简历。使用STAR格式描述经历，包含量化数据。输出Markdown格式。"
- 重写模式：携带原始简历 + ATS 报告 + 缺失关键词，要求逐段重写

---

## 功能4：JD 智能解析 + 雷达图

### 后端
- `JdAnalyzerService.java` — 核心服务
  - `analyzeJd(jdText, resumeText)` → 解析 JD 结构化信息 + 多维匹配度
  - 返回 6 个维度的评分：技术栈匹配、经验年限、项目复杂度、软技能、行业背景、教育背景
  - 本地规则 + LLM 混合评分
- DTO: `JdAnalyzeRequest`, `JdRadarResponse`（含 dimensions 数组，每项有 name/score/maxScore/detail）
- API: `POST /api/jd/analyze`

### 前端 JdRadar.vue
- 输入区：粘贴 JD + 上传简历（或选择历史分析记录）
- 结果区：
  - SVG 雷达图（6 个维度，纯 Vue 绘制，不引入图表库）
  - 每个维度的详细说明和改进建议
  - 总体匹配度百分比

### 雷达图实现
- 纯 SVG + Vue 计算属性，不需要 echarts 等重依赖
- 6 个顶点坐标通过三角函数计算
- 两层多边形：外层满分轮廓、内层实际得分

---

## 功能5：简历真实性检测

### 后端
- `ResumeAuditService.java` — 核心服务
  - `audit(resumeText, targetRole)` → 检测简历中的可疑点
  - 本地规则检测：时间线矛盾、数据异常（如"提升10000%"）、技术栈与经验不匹配
  - LLM 辅助检测：逻辑一致性、描述合理性
  - 返回风险等级（低/中/高）+ 具体可疑项列表
- DTO: `AuditRequest`, `AuditResponse`（含 riskLevel, auditItems 列表，每项有 category/description/severity/suggestion）
- API: `POST /api/resume/audit`

### 前端 ResumeAudit.vue
- 输入区：上传简历文件或粘贴文本 + 目标岗位
- 结果区：
  - 总体风险等级徽章（绿/黄/红）
  - 可疑项列表，每项显示类别图标、描述、严重程度、修改建议
  - "一键修复建议"按钮（调用 LLM 生成修复后的文本）

---

## 数据库新增表

```sql
-- 模拟面试会话
CREATE TABLE mock_interview_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_role VARCHAR(255),
    resume_text TEXT,
    jd_text TEXT,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    total_score DOUBLE,
    question_count INT DEFAULT 0,
    created_at TIMESTAMP,
    finished_at TIMESTAMP
);

-- 模拟面试消息
CREATE TABLE mock_interview_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    score INT,
    created_at TIMESTAMP
);

-- 对话优化会话
CREATE TABLE chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    analysis_id BIGINT,
    created_at TIMESTAMP
);

-- 对话优化消息
CREATE TABLE chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP
);
```

JD 解析和简历审计不需要新表，结果直接返回不持久化。

## 实现顺序

1. **功能1：AI 模拟面试** — 新增 2 表 + 1 Service + 4 API + 1 组件
2. **功能2：多轮对话助手** — 新增 2 表 + 1 Service + 3 API + 1 组件
3. **功能3：简历生成/重写** — 新增 1 Service + 2 API + 1 组件
4. **功能4：JD 雷达图** — 新增 1 Service + 1 API + 1 组件（含 SVG 雷达图）
5. **功能5：真实性检测** — 新增 1 Service + 1 API + 1 组件

## 共享基础设施

- LLM 调用：复用现有 `ConfigService` 获取 apiKey/model/baseUrl，抽取公共 `LlmClient` 工具类
- 认证：复用现有 `AuthService.requireUser()`
- 前端 HTTP：复用现有 `authHeaders()` + fetch 封装
