package com.resumeai.controller;

import com.resumeai.dto.AdminConfigRequest;
import com.resumeai.dto.AdminConfigResponse;
import com.resumeai.dto.AdminUserItem;
import com.resumeai.dto.AdminUsersResponse;
import com.resumeai.dto.AdminUserUpdateRequest;
import com.resumeai.dto.AnalysisHistoryResponse;
import com.resumeai.dto.AnalyzeEnqueueResponse;
import com.resumeai.dto.AnalyzeJobStatusResponse;
import com.resumeai.dto.AuthRequest;
import com.resumeai.dto.AuthResponse;
import com.resumeai.dto.InterviewKbCrawlRequest;
import com.resumeai.dto.InterviewKbCrawlResponse;
import com.resumeai.dto.InterviewKbDocItem;
import com.resumeai.dto.InterviewKbDocQuestionsResponse;
import com.resumeai.dto.InterviewKbDocsResponse;
import com.resumeai.dto.InterviewKbLlmImportRequest;
import com.resumeai.dto.InterviewKbLlmImportResponse;
import com.resumeai.dto.InterviewSessionResponse;
import com.resumeai.dto.InterviewAnswerRequest;
import com.resumeai.dto.StartInterviewRequest;
import com.resumeai.dto.UserInfoResponse;
import com.resumeai.model.UserAccount;
import com.resumeai.service.AnalysisQueueService;
import com.resumeai.service.AnalyzeService;
import com.resumeai.service.AuthService;
import com.resumeai.service.ConfigService;
import com.resumeai.service.ForbiddenException;
import com.resumeai.service.HistoryService;
import com.resumeai.service.InterviewKbCrawlerService;
import com.resumeai.service.InterviewKbDocViewService;
import com.resumeai.service.InterviewKbLlmImportService;
import com.resumeai.service.InterviewQuestionKbService;
import com.resumeai.service.MockInterviewService;
import com.resumeai.service.UnauthorizedException;
import com.resumeai.service.UserManageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 统一 API 控制器，所有 REST 接口的入口。
 * <p>
 * 路由分组：
 * - auth/*      — 用户注册、登录、登出、当前用户信息
 * - analyze/*   — 简历分析任务提交与状态查询
 * - analyses/*  — 历史分析记录查询
 * - admin/*     — 管理员配置、用户管理、面试题库管理
 * <p>
 * 全局异常处理在本类底部统一映射为 { "detail": "..." } 格式，方便前端直接展示错误信息。
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);
    private final AnalysisQueueService analysisQueueService;
    private final AnalyzeService analyzeService;
    private final ConfigService configService;
    private final AuthService authService;
    private final HistoryService historyService;
    private final UserManageService userManageService;
    private final InterviewQuestionKbService interviewQuestionKbService;
    private final InterviewKbCrawlerService interviewKbCrawlerService;
    private final InterviewKbDocViewService interviewKbDocViewService;
    private final InterviewKbLlmImportService interviewKbLlmImportService;
    private final MockInterviewService mockInterviewService;

    /** 构造函数，通过 Spring 依赖注入获取所有业务服务。 */
    public ApiController(
            AnalysisQueueService analysisQueueService,
            AnalyzeService analyzeService,
            ConfigService configService,
            AuthService authService,
            HistoryService historyService,
            UserManageService userManageService,
            InterviewQuestionKbService interviewQuestionKbService,
            InterviewKbCrawlerService interviewKbCrawlerService,
            InterviewKbDocViewService interviewKbDocViewService,
            InterviewKbLlmImportService interviewKbLlmImportService,
            MockInterviewService mockInterviewService) {
        this.analysisQueueService = analysisQueueService;
        this.analyzeService = analyzeService;
        this.configService = configService;
        this.authService = authService;
        this.historyService = historyService;
        this.userManageService = userManageService;
        this.interviewQuestionKbService = interviewQuestionKbService;
        this.interviewKbCrawlerService = interviewKbCrawlerService;
        this.interviewKbDocViewService = interviewKbDocViewService;
        this.interviewKbLlmImportService = interviewKbLlmImportService;
        this.mockInterviewService = mockInterviewService;
    }

    /** 健康检查接口，返回服务名称和当前时间戳。 */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "ok", true,
                "app", "Resume AI Java Service",
                "time", Instant.now().toString()
        );
    }

    /**
     * JSON 注册入口（前端默认调用）。
     * @param req 包含 username 和 password 的注册请求
     * @param request HTTP 请求，用于提取客户端 IP 做注册频率限制
     * @return 注册成功后的认证令牌和用户信息
     */
    @PostMapping(value = "/auth/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthResponse register(@RequestBody @Valid AuthRequest req, HttpServletRequest request) {
        String ip = authService.getClientIp(request);
        return authService.register(req, ip);
    }

    /** 表单注册入口（兼容移动端/低代码表单提交）。 */
    @PostMapping(value = "/auth/register", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public AuthResponse registerForm(@Valid AuthRequest req, HttpServletRequest request) {
        String ip = authService.getClientIp(request);
        return authService.register(req, ip);
    }

    /**
     * JSON 登录入口。
     * @param req 包含 username 和 password 的登录请求
     * @return 登录成功后的认证令牌和用户信息
     */
    @PostMapping(value = "/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthResponse login(@RequestBody @Valid AuthRequest req) {
        return authService.login(req);
    }

    /** 表单登录入口（兼容性保留）。 */
    @PostMapping(value = "/auth/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public AuthResponse loginForm(@Valid AuthRequest req) {
        return authService.login(req);
    }

    /**
     * 获取当前登录用户信息。
     * @param request HTTP 请求，从中解析认证令牌
     * @return 当前用户的基本信息（id、用户名、角色、VIP 状态等）
     */
    @GetMapping("/auth/me")
    public UserInfoResponse me(HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return authService.toUserInfo(user);
    }

    /** 登出接口，删除当前会话令牌。 */
    @PostMapping("/auth/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        authService.logout(request);
        return Map.of("ok", true);
    }

    /**
     * 提交简历分析任务（异步入队）。
     * 分析任务只负责"入队"，实际分析由队列消费者异步执行。
     * @param file 简历文件（PDF/DOCX/图片/纯文本）
     * @param jdText 职位描述文本
     * @param targetRole 目标岗位名称（可选）
     * @param jdImage JD 截图（可选，用于 OCR 提取文本）
     * @return 入队结果，包含 jobId 和队列位置
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalyzeEnqueueResponse analyze(
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "jd_text") String jdText,
            @RequestParam(name = "target_role", required = false) String targetRole,
            @RequestPart(name = "jd_image", required = false) MultipartFile jdImage,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return analysisQueueService.enqueue(file, jdText, targetRole, jdImage, user);
    }

    /**
     * 查询分析任务状态。
     * @param jobId 任务 ID
     * @return 任务状态（排队中/处理中/完成/失败），完成时包含分析结果
     */
    @GetMapping("/analyze/jobs/{jobId}")
    public AnalyzeJobStatusResponse analyzeJobStatus(
            @PathVariable("jobId") String jobId,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        boolean adminMode = "ADMIN".equalsIgnoreCase(user.getRole());
        return analysisQueueService.getJobStatus(jobId, user, adminMode);
    }

    /**
     * 查询当前用户的历史分析记录（分页）。
     * @param page 页码（从 0 开始）
     * @param size 每页条数
     * @return 分页后的分析历史列表
     */
    @GetMapping("/analyses/mine")
    public AnalysisHistoryResponse myAnalyses(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return historyService.listMine(user.getId(), page, size);
    }

    /** 获取管理员配置（AI 模型参数等）。 */
    @GetMapping("/admin/config")
    public AdminConfigResponse getConfig(HttpServletRequest request) {
        authService.requireAdmin(request);
        return configService.getConfig();
    }

    /** 更新管理员配置。 */
    @PutMapping("/admin/config")
    public AdminConfigResponse updateConfig(@RequestBody @Valid AdminConfigRequest req, HttpServletRequest request) {
        authService.requireAdmin(request);
        return configService.updateConfig(req);
    }

    /** 管理员查看所有用户的分析记录（可按用户名筛选，分页）。 */
    @GetMapping("/admin/analyses")
    public AnalysisHistoryResponse adminAnalyses(
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            HttpServletRequest request) {
        authService.requireAdmin(request);
        return historyService.listAllForAdmin(username, page, size);
    }

    /** 管理员查看用户列表（可按关键词搜索，分页）。 */
    @GetMapping("/admin/users")
    public AdminUsersResponse adminUsers(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            HttpServletRequest request) {
        authService.requireAdmin(request);
        return userManageService.listUsers(keyword, page, size);
    }

    /**
     * 管理员更新用户状态（如 VIP、黑名单等）。
     * @param userId 用户 ID
     * @param req 更新请求体
     * @return 更新后的用户信息
     */
    @PutMapping("/admin/users/{id}")
    public AdminUserItem updateAdminUser(
            @PathVariable("id") Long userId,
            @RequestBody @Valid AdminUserUpdateRequest req,
            HttpServletRequest request) {
        authService.requireAdmin(request);
        return userManageService.updateUserStatus(userId, req);
    }

    /** 上传面试题库文档（支持多种文件格式）。 */
    @PostMapping(value = "/admin/interview-kb/docs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InterviewKbDocItem uploadInterviewDoc(
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "title", required = false) String title,
            HttpServletRequest request) {
        UserAccount admin = authService.requireAdmin(request);
        return interviewQuestionKbService.uploadDoc(file, title, admin);
    }

    /** 分页查询面试题库文档列表。 */
    @GetMapping("/admin/interview-kb/docs")
    public InterviewKbDocsResponse interviewDocs(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            HttpServletRequest request) {
        authService.requireAdmin(request);
        return interviewQuestionKbService.listDocs(page, size);
    }

    /** 删除指定面试题库文档。 */
    @DeleteMapping("/admin/interview-kb/docs/{id}")
    public Map<String, Object> deleteInterviewDoc(
            @PathVariable("id") Long docId,
            HttpServletRequest request) {
        authService.requireAdmin(request);
        interviewQuestionKbService.deleteDoc(docId);
        return Map.of("ok", true);
    }

    /** 分页查看题库文档中的题目文本。 */
    @GetMapping("/admin/interview-kb/docs/{id}/questions")
    public InterviewKbDocQuestionsResponse interviewDocQuestions(
            @PathVariable("id") Long docId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size,
            HttpServletRequest request) {
        authService.requireAdmin(request);
        return interviewKbDocViewService.listQuestions(docId, page, size);
    }

    /** 爬虫入库接口，支持 authCookie/referer/authHeader 访问私有页面。 */
    @PostMapping("/admin/interview-kb/crawl")
    public InterviewKbCrawlResponse crawlInterviewKb(
            @RequestBody @Valid InterviewKbCrawlRequest req,
            HttpServletRequest request) {
        UserAccount admin = authService.requireAdmin(request);
        return interviewKbCrawlerService.crawlAndImport(req, admin);
    }

    /** 通过大模型批量导入面试题到题库。 */
    @PostMapping("/admin/interview-kb/llm-import")
    public InterviewKbLlmImportResponse llmImportInterviewKb(
            @RequestBody @Valid InterviewKbLlmImportRequest req,
            HttpServletRequest request) {
        UserAccount admin = authService.requireAdmin(request);
        return interviewKbLlmImportService.importByLlm(req, admin);
    }

    // ==================== AI 模拟面试 ====================

    /** 开始一场模拟面试。 */
    @PostMapping(value = "/interview/start", consumes = MediaType.APPLICATION_JSON_VALUE)
    public InterviewSessionResponse startInterview(
            @RequestBody @Valid StartInterviewRequest req,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return mockInterviewService.startInterview(
                req.getTargetRole(),
                req.getResumeText(),
                req.getJdText(),
                req.getMissingKeywords(),
                user);
    }

    /** 用户回答面试问题，AI 评分并出下一题。 */
    @PostMapping(value = "/interview/start", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InterviewSessionResponse startInterviewMultipart(
            @RequestParam(name = "target_role") String targetRole,
            @RequestParam(name = "resume_text", required = false) String resumeText,
            @RequestParam(name = "jd_text", required = false) String jdText,
            @RequestParam(name = "missing_keywords", required = false) java.util.List<String> missingKeywords,
            @RequestPart(name = "jd_image", required = false) MultipartFile jdImage,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        String resolvedJd = analyzeService.resolveJdText(jdText, jdImage);
        return mockInterviewService.startInterview(
                targetRole,
                resumeText,
                resolvedJd,
                missingKeywords,
                user);
    }

    @PostMapping("/interview/{sessionId}/answer")
    public InterviewSessionResponse answerInterview(
            @PathVariable("sessionId") Long sessionId,
            @RequestBody @Valid InterviewAnswerRequest req,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return mockInterviewService.answer(sessionId, req.getAnswer(), user);
    }

    /** 鐢ㄦ埛鍥炵瓟闈㈣瘯闂锛孉I 浠?SSE 娴佸紡杩斿洖璇勫垎涓庝笅涓€棰樸€?*/
    @PostMapping(value = "/interview/{sessionId}/answer/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter answerInterviewStream(
            @PathVariable("sessionId") Long sessionId,
            @RequestBody @Valid InterviewAnswerRequest req,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        SseEmitter emitter = new SseEmitter(120000L);
        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event().name("status").data(Map.of("stage", "received")));
                emitter.send(SseEmitter.event().name("status").data(Map.of("stage", "evaluating")));

                InterviewSessionResponse response = mockInterviewService.answer(sessionId, req.getAnswer(), user);
                String content = response.getLatestAiMessage() == null
                        ? ""
                        : String.valueOf(response.getLatestAiMessage().getContent());
                if (!content.isBlank()) {
                    int chunkSize = 28;
                    for (int i = 0; i < content.length(); i += chunkSize) {
                        String chunk = content.substring(i, Math.min(content.length(), i + chunkSize));
                        emitter.send(SseEmitter.event().name("delta").data(Map.of("content", chunk)));
                    }
                }
                emitter.send(SseEmitter.event().name("done").data(response));
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(Map.of("detail", ex.getMessage() == null ? "interview stream failed" : ex.getMessage())));
                } catch (Exception ignore) {
                    // ignore secondary failure
                }
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    /** 结束面试，生成总评报告。 */
    @PostMapping("/interview/{sessionId}/end")
    public InterviewSessionResponse endInterview(
            @PathVariable("sessionId") Long sessionId,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return mockInterviewService.endInterview(sessionId, user);
    }

    /** 查询面试会话的完整消息列表。 */
    @GetMapping("/interview/{sessionId}/messages")
    public InterviewSessionResponse interviewMessages(
            @PathVariable("sessionId") Long sessionId,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return mockInterviewService.getMessages(sessionId, user);
    }

    /** 查询当前用户的面试历史列表。 */
    @GetMapping("/interview/sessions")
    public java.util.List<InterviewSessionResponse> interviewSessions(HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return mockInterviewService.listSessions(user);
    }

    // ==================== 全局异常处理 ====================

    /** 处理参数非法异常，返回 400。 */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArg(IllegalArgumentException ex) {
        return Map.of("detail", ex.getMessage());
    }

    /** 处理未认证异常，返回 401。 */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleUnauthorized(UnauthorizedException ex) {
        return Map.of("detail", ex.getMessage());
    }

    /** 处理权限不足异常，返回 403。 */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> handleForbidden(ForbiddenException ex) {
        return Map.of("detail", ex.getMessage());
    }

    /** 处理请求体 JSON 解析失败，返回 400。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleJsonParseError(HttpMessageNotReadableException ex) {
        return Map.of("detail", "request body must be valid json");
    }

    /** 处理方法参数校验失败（@Valid），返回 400。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return Map.of("detail", firstValidationMessage(ex.getBindingResult()));
    }

    /** 处理表单绑定校验失败，返回 400。 */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBind(BindException ex) {
        return Map.of("detail", firstValidationMessage(ex.getBindingResult()));
    }

    /** 处理不支持的 Content-Type，返回 400。 */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return Map.of("detail", "unsupported content type, use application/json or application/x-www-form-urlencoded");
    }

    /** 兜底异常处理，记录日志并返回 500。 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGeneral(Exception ex) {
        log.error("Unhandled error in API", ex);
        return Map.of("detail", "internal server error");
    }

    /**
     * 从校验结果中提取第一条错误信息，用于统一错误响应。
     * @param bindingResult Spring 校验绑定结果
     * @return 人类可读的错误描述
     */
    private String firstValidationMessage(BindingResult bindingResult) {
        FieldError fieldError = bindingResult.getFieldError();
        if (fieldError == null) {
            return "request validation failed";
        }
        String field = fieldError.getField();
        if ("username".equals(field)) {
            return "username format invalid, use 4-32 letters/numbers/_";
        }
        if ("password".equals(field)) {
            return "password must be at least 8 characters";
        }
        String defaultMessage = fieldError.getDefaultMessage();
        if (defaultMessage == null || defaultMessage.isBlank()) {
            return "request validation failed";
        }
        return defaultMessage;
    }
}
