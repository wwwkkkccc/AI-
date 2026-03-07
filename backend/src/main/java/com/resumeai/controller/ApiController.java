package com.resumeai.controller;

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
import com.resumeai.dto.JdAnalyzeRequest;
import com.resumeai.dto.JdRadarResponse;
import com.resumeai.dto.ResumeAuditRequest;
import com.resumeai.dto.ResumeAuditResponse;
import com.resumeai.dto.RewriteResumeRequest;
import com.resumeai.dto.StartChatRequest;
import com.resumeai.dto.UserInfoResponse;
import com.resumeai.model.UserAccount;
import com.resumeai.service.AnalysisQueueService;
import com.resumeai.service.AuditLogService;
import com.resumeai.service.AuthService;
import com.resumeai.service.HistoryService;
import com.resumeai.service.JdAnalyzerService;
import com.resumeai.service.RateLimitService;
import com.resumeai.service.ResumeAuditService;
import com.resumeai.service.ResumeChatService;
import com.resumeai.service.ResumeGeneratorService;
import com.resumeai.service.TooManyRequestsException;
import com.resumeai.service.UserManageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final AnalysisQueueService analysisQueueService;
    private final AuthService authService;
    private final HistoryService historyService;
    private final UserManageService userManageService;
    private final ResumeGeneratorService resumeGeneratorService;
    private final ResumeChatService resumeChatService;
    private final JdAnalyzerService jdAnalyzerService;
    private final ResumeAuditService resumeAuditService;
    private final RateLimitService rateLimitService;
    private final AuditLogService auditLogService;

    @Value("${app.rate-limit.analyze-per-hour:5}")
    private int analyzePerHour;

    @Value("${app.rate-limit.generate-per-hour:10}")
    private int generatePerHour;

    @Value("${app.rate-limit.chat-per-hour:30}")
    private int chatPerHour;

    public ApiController(
            AnalysisQueueService analysisQueueService,
            AuthService authService,
            HistoryService historyService,
            UserManageService userManageService,
            ResumeGeneratorService resumeGeneratorService,
            ResumeChatService resumeChatService,
            JdAnalyzerService jdAnalyzerService,
            ResumeAuditService resumeAuditService,
            RateLimitService rateLimitService,
            AuditLogService auditLogService) {
        this.analysisQueueService = analysisQueueService;
        this.authService = authService;
        this.historyService = historyService;
        this.userManageService = userManageService;
        this.resumeGeneratorService = resumeGeneratorService;
        this.resumeChatService = resumeChatService;
        this.jdAnalyzerService = jdAnalyzerService;
        this.resumeAuditService = resumeAuditService;
        this.rateLimitService = rateLimitService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "ok", true,
                "app", "Resume AI Java Service",
                "time", Instant.now().toString());
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
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        if (!rateLimitService.checkRateLimit(user.getId(), "analyze", analyzePerHour, 3600, isAdmin)) {
            throw new TooManyRequestsException("analyze request too frequent");
        }
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
        UserAccount user = authService.requireUser(request);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        if (!rateLimitService.checkRateLimit(user.getId(), "generate", generatePerHour, 3600, isAdmin)) {
            throw new TooManyRequestsException("generate request too frequent");
        }
        return resumeGeneratorService.generateFromJd(req.getTargetRole(), req.getJdText(), req.getUserBackground(), user.getId());
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
        return resumeGeneratorService.rewriteByRawText(req.getResumeText(), req.getJdText(), req.getTargetRole(), user.getId());
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
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());
        if (!rateLimitService.checkRateLimit(user.getId(), "chat", chatPerHour, 3600, isAdmin)) {
            throw new TooManyRequestsException("chat request too frequent");
        }
        return resumeChatService.sendMessage(sessionId, req.getMessage(), user, isAdmin);
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
        UserAccount admin = authService.requireAdmin(request);
        AdminUserItem result = userManageService.updateUserStatus(userId, req);
        String ip = authService.getClientIp(request);
        String detail = String.format("blacklisted=%s, vip=%s", req.getBlacklisted(), req.getVip());
        auditLogService.log(admin, "UPDATE_USER", "USER", userId.toString(), detail, ip);
        return result;
    }
}
