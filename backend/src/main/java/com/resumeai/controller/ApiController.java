package com.resumeai.controller;

import com.resumeai.dto.AdminConfigRequest;
import com.resumeai.dto.AdminConfigResponse;
import com.resumeai.dto.AdminUserItem;
import com.resumeai.dto.AdminUserUpdateRequest;
import com.resumeai.dto.AdminUsersResponse;
import com.resumeai.dto.AnalysisHistoryResponse;
import com.resumeai.dto.AnalyzeEnqueueResponse;
import com.resumeai.dto.AnalyzeJobStatusResponse;
import com.resumeai.dto.AuthRequest;
import com.resumeai.dto.AuthResponse;
import com.resumeai.dto.ChatMessageRequest;
import com.resumeai.dto.ChatSessionResponse;
import com.resumeai.dto.GenerateResumeRequest;
import com.resumeai.dto.GeneratedResumeResponse;
import com.resumeai.dto.InterviewKbCrawlRequest;
import com.resumeai.dto.InterviewKbCrawlResponse;
import com.resumeai.dto.InterviewKbDocItem;
import com.resumeai.dto.InterviewKbDocQuestionsResponse;
import com.resumeai.dto.InterviewKbDocsResponse;
import com.resumeai.dto.InterviewKbLlmImportRequest;
import com.resumeai.dto.InterviewKbLlmImportResponse;
import com.resumeai.dto.JdAnalyzeRequest;
import com.resumeai.dto.JdRadarResponse;
import com.resumeai.dto.ResumeAuditRequest;
import com.resumeai.dto.ResumeAuditResponse;
import com.resumeai.dto.RewriteResumeRequest;
import com.resumeai.dto.StartChatRequest;
import com.resumeai.dto.UserInfoResponse;
import com.resumeai.model.UserAccount;
import com.resumeai.service.AnalysisQueueService;
import com.resumeai.service.AuthService;
import com.resumeai.service.ConfigService;
import com.resumeai.service.ForbiddenException;
import com.resumeai.service.HistoryService;
import com.resumeai.service.InterviewKbCrawlerService;
import com.resumeai.service.InterviewKbDocViewService;
import com.resumeai.service.InterviewKbLlmImportService;
import com.resumeai.service.InterviewQuestionKbService;
import com.resumeai.service.JdAnalyzerService;
import com.resumeai.service.ResumeAuditService;
import com.resumeai.service.ResumeChatService;
import com.resumeai.service.ResumeGeneratorService;
import com.resumeai.service.UnauthorizedException;
import com.resumeai.service.UserManageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    private final AnalysisQueueService analysisQueueService;
    private final ConfigService configService;
    private final AuthService authService;
    private final HistoryService historyService;
    private final UserManageService userManageService;
    private final InterviewQuestionKbService interviewQuestionKbService;
    private final InterviewKbCrawlerService interviewKbCrawlerService;
    private final InterviewKbDocViewService interviewKbDocViewService;
    private final InterviewKbLlmImportService interviewKbLlmImportService;
    private final ResumeGeneratorService resumeGeneratorService;
    private final ResumeChatService resumeChatService;
    private final JdAnalyzerService jdAnalyzerService;
    private final ResumeAuditService resumeAuditService;

    public ApiController(
            AnalysisQueueService analysisQueueService,
            ConfigService configService,
            AuthService authService,
            HistoryService historyService,
            UserManageService userManageService,
            InterviewQuestionKbService interviewQuestionKbService,
            InterviewKbCrawlerService interviewKbCrawlerService,
            InterviewKbDocViewService interviewKbDocViewService,
            InterviewKbLlmImportService interviewKbLlmImportService,
            ResumeGeneratorService resumeGeneratorService,
            ResumeChatService resumeChatService,
            JdAnalyzerService jdAnalyzerService,
            ResumeAuditService resumeAuditService) {
        this.analysisQueueService = analysisQueueService;
        this.configService = configService;
        this.authService = authService;
        this.historyService = historyService;
        this.userManageService = userManageService;
        this.interviewQuestionKbService = interviewQuestionKbService;
        this.interviewKbCrawlerService = interviewKbCrawlerService;
        this.interviewKbDocViewService = interviewKbDocViewService;
        this.interviewKbLlmImportService = interviewKbLlmImportService;
        this.resumeGeneratorService = resumeGeneratorService;
        this.resumeChatService = resumeChatService;
        this.jdAnalyzerService = jdAnalyzerService;
        this.resumeAuditService = resumeAuditService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "ok", true,
                "app", "Resume AI Java Service",
                "time", Instant.now().toString()
        );
    }

    @PostMapping(value = "/auth/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthResponse register(@RequestBody @Valid AuthRequest req, HttpServletRequest request) {
        String ip = authService.getClientIp(request);
        return authService.register(req, ip);
    }

    @PostMapping(value = "/auth/register", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public AuthResponse registerForm(@Valid AuthRequest req, HttpServletRequest request) {
        String ip = authService.getClientIp(request);
        return authService.register(req, ip);
    }

    @PostMapping(value = "/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthResponse login(@RequestBody @Valid AuthRequest req) {
        return authService.login(req);
    }

    @PostMapping(value = "/auth/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public AuthResponse loginForm(@Valid AuthRequest req) {
        return authService.login(req);
    }

    @GetMapping("/auth/me")
    public UserInfoResponse me(HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return authService.toUserInfo(user);
    }

    @PostMapping("/auth/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        authService.logout(request);
        return Map.of("ok", true);
    }

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

    @GetMapping("/analyze/jobs/{jobId}")
    public AnalyzeJobStatusResponse analyzeJobStatus(
            @PathVariable("jobId") String jobId,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        boolean adminMode = "ADMIN".equalsIgnoreCase(user.getRole());
        return analysisQueueService.getJobStatus(jobId, user, adminMode);
    }

    @GetMapping("/analyses/mine")
    public AnalysisHistoryResponse myAnalyses(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return historyService.listMine(user.getId(), page, size);
    }

    @PostMapping("/resume/generate")
    public GeneratedResumeResponse generateResume(
            @RequestBody @Valid GenerateResumeRequest req,
            HttpServletRequest request) {
        authService.requireUser(request);
        return resumeGeneratorService.generateFromJd(req.getTargetRole(), req.getJdText(), req.getUserBackground());
    }

    @PostMapping("/resume/rewrite")
    public GeneratedResumeResponse rewriteResume(
            @RequestBody RewriteResumeRequest req,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        boolean adminMode = "ADMIN".equalsIgnoreCase(user.getRole());
        if (req.getAnalysisId() != null) {
            return resumeGeneratorService.rewriteByAnalysis(req.getAnalysisId(), user, adminMode);
        }
        return resumeGeneratorService.rewriteByRawText(req.getResumeText(), req.getJdText(), req.getTargetRole());
    }

    @PostMapping("/chat/start")
    public ChatSessionResponse startChat(
            @RequestBody(required = false) StartChatRequest req,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        boolean adminMode = "ADMIN".equalsIgnoreCase(user.getRole());
        Long analysisId = req == null ? null : req.getAnalysisId();
        return resumeChatService.startSession(analysisId, user, adminMode);
    }

    @PostMapping("/chat/{sessionId}/message")
    public ChatSessionResponse sendChatMessage(
            @PathVariable("sessionId") Long sessionId,
            @RequestBody @Valid ChatMessageRequest req,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        boolean adminMode = "ADMIN".equalsIgnoreCase(user.getRole());
        return resumeChatService.sendMessage(sessionId, req.getMessage(), user, adminMode);
    }

    @GetMapping("/chat/{sessionId}/messages")
    public ChatSessionResponse chatMessages(
            @PathVariable("sessionId") Long sessionId,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        boolean adminMode = "ADMIN".equalsIgnoreCase(user.getRole());
        return resumeChatService.getMessages(sessionId, user, adminMode);
    }

    @GetMapping("/chat/sessions")
    public java.util.List<ChatSessionResponse> myChatSessions(HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return resumeChatService.listMySessions(user);
    }

    @PostMapping("/jd/analyze")
    public JdRadarResponse analyzeJd(
            @RequestBody JdAnalyzeRequest req,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        boolean adminMode = "ADMIN".equalsIgnoreCase(user.getRole());
        return jdAnalyzerService.analyze(req, user, adminMode);
    }

    @PostMapping("/resume/audit")
    public ResumeAuditResponse auditResume(
            @RequestBody ResumeAuditRequest req,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        boolean adminMode = "ADMIN".equalsIgnoreCase(user.getRole());
        return resumeAuditService.audit(req, user, adminMode);
    }

    @GetMapping("/admin/config")
    public AdminConfigResponse getConfig(HttpServletRequest request) {
        authService.requireAdmin(request);
        return configService.getConfig();
    }

    @PutMapping("/admin/config")
    public AdminConfigResponse updateConfig(@RequestBody @Valid AdminConfigRequest req, HttpServletRequest request) {
        authService.requireAdmin(request);
        return configService.updateConfig(req);
    }

    @GetMapping("/admin/analyses")
    public AnalysisHistoryResponse adminAnalyses(
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            HttpServletRequest request) {
        authService.requireAdmin(request);
        return historyService.listAllForAdmin(username, page, size);
    }

    @GetMapping("/admin/users")
    public AdminUsersResponse adminUsers(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            HttpServletRequest request) {
        authService.requireAdmin(request);
        return userManageService.listUsers(keyword, page, size);
    }

    @PutMapping("/admin/users/{id}")
    public AdminUserItem updateAdminUser(
            @PathVariable("id") Long userId,
            @RequestBody @Valid AdminUserUpdateRequest req,
            HttpServletRequest request) {
        authService.requireAdmin(request);
        return userManageService.updateUserStatus(userId, req);
    }

    @PostMapping(value = "/admin/interview-kb/docs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InterviewKbDocItem uploadInterviewDoc(
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "title", required = false) String title,
            HttpServletRequest request) {
        UserAccount admin = authService.requireAdmin(request);
        return interviewQuestionKbService.uploadDoc(file, title, admin);
    }

    @GetMapping("/admin/interview-kb/docs")
    public InterviewKbDocsResponse interviewDocs(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            HttpServletRequest request) {
        authService.requireAdmin(request);
        return interviewQuestionKbService.listDocs(page, size);
    }

    @DeleteMapping("/admin/interview-kb/docs/{id}")
    public Map<String, Object> deleteInterviewDoc(
            @PathVariable("id") Long docId,
            HttpServletRequest request) {
        authService.requireAdmin(request);
        interviewQuestionKbService.deleteDoc(docId);
        return Map.of("ok", true);
    }

    @GetMapping("/admin/interview-kb/docs/{id}/questions")
    public InterviewKbDocQuestionsResponse interviewDocQuestions(
            @PathVariable("id") Long docId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size,
            HttpServletRequest request) {
        authService.requireAdmin(request);
        return interviewKbDocViewService.listQuestions(docId, page, size);
    }

    @PostMapping("/admin/interview-kb/crawl")
    public InterviewKbCrawlResponse crawlInterviewKb(
            @RequestBody @Valid InterviewKbCrawlRequest req,
            HttpServletRequest request) {
        UserAccount admin = authService.requireAdmin(request);
        return interviewKbCrawlerService.crawlAndImport(req, admin);
    }

    @PostMapping("/admin/interview-kb/llm-import")
    public InterviewKbLlmImportResponse llmImportInterviewKb(
            @RequestBody @Valid InterviewKbLlmImportRequest req,
            HttpServletRequest request) {
        UserAccount admin = authService.requireAdmin(request);
        return interviewKbLlmImportService.importByLlm(req, admin);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArg(IllegalArgumentException ex) {
        return Map.of("detail", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleUnauthorized(UnauthorizedException ex) {
        return Map.of("detail", ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> handleForbidden(ForbiddenException ex) {
        return Map.of("detail", ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleJsonParseError(HttpMessageNotReadableException ex) {
        return Map.of("detail", "request body must be valid json");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return Map.of("detail", firstValidationMessage(ex.getBindingResult()));
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBind(BindException ex) {
        return Map.of("detail", firstValidationMessage(ex.getBindingResult()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return Map.of("detail", "unsupported content type, use application/json or application/x-www-form-urlencoded");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGeneral(Exception ex) {
        log.error("Unhandled error in API", ex);
        return Map.of("detail", "internal server error");
    }

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
