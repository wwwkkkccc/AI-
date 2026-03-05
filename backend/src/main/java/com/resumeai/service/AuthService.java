package com.resumeai.service;

import com.resumeai.dto.AuthRequest;
import com.resumeai.dto.AuthResponse;
import com.resumeai.dto.UserInfoResponse;
import com.resumeai.model.RegisterIpDaily;
import com.resumeai.model.UserAccount;
import com.resumeai.model.UserSession;
import com.resumeai.repository.RegisterIpDailyRepository;
import com.resumeai.repository.UserAccountRepository;
import com.resumeai.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务，负责用户注册、登录、登出、会话管理及权限校验。
 * <p>
 * 主要职责：
 * 1. 用户注册（含同 IP 每日注册次数限制）
 * 2. 用户登录与密码校验
 * 3. 基于 Token 的会话创建、验证与销毁
 * 4. 管理员账号自动初始化
 * 5. 用户/管理员身份鉴权
 * </p>
 */
@Service
public class AuthService {
    /** 用户名格式：4-32 位字母、数字或下划线 */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,32}$");
    /** 管理员角色标识 */
    private static final String ROLE_ADMIN = "ADMIN";
    /** 普通用户角色标识 */
    private static final String ROLE_USER = "USER";

    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final RegisterIpDailyRepository registerIpDailyRepository;
    /** BCrypt 密码编码器，用于密码哈希与校验 */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    /** 安全随机数生成器，用于生成 Token */
    private final SecureRandom secureRandom = new SecureRandom();
    /** 业务时区，用于按天统计注册次数 */
    private final ZoneId zoneId;
    /** Token 过期时间（小时） */
    private final long tokenExpireHours;
    /** 同一 IP 每天最大注册次数 */
    private final int maxRegisterPerIpPerDay;
    /** 预设管理员用户名 */
    private final String adminUsername;
    /** 预设管理员密码 */
    private final String adminPassword;

    /**
     * 构造方法，注入依赖与配置参数。
     *
     * @param userAccountRepository    用户账号仓库
     * @param userSessionRepository    用户会话仓库
     * @param registerIpDailyRepository IP 每日注册统计仓库
     * @param timezone                 业务时区配置
     * @param tokenExpireHours         Token 过期小时数
     * @param maxRegisterPerIpPerDay   同 IP 每日最大注册数
     * @param adminUsername            预设管理员用户名
     * @param adminPassword            预设管理员密码
     */
    public AuthService(
            UserAccountRepository userAccountRepository,
            UserSessionRepository userSessionRepository,
            RegisterIpDailyRepository registerIpDailyRepository,
            @Value("${app.auth.timezone:Asia/Shanghai}") String timezone,
            @Value("${app.auth.token-expire-hours:720}") long tokenExpireHours,
            @Value("${app.auth.max-register-per-ip-per-day:3}") int maxRegisterPerIpPerDay,
            @Value("${app.auth.admin-username:admin}") String adminUsername,
            @Value("${app.auth.admin-password:Admin@123456}") String adminPassword) {
        this.userAccountRepository = userAccountRepository;
        this.userSessionRepository = userSessionRepository;
        this.registerIpDailyRepository = registerIpDailyRepository;
        this.zoneId = ZoneId.of(clean(timezone).isEmpty() ? "Asia/Shanghai" : clean(timezone));
        this.tokenExpireHours = Math.max(24, tokenExpireHours);
        this.maxRegisterPerIpPerDay = Math.max(1, maxRegisterPerIpPerDay);
        this.adminUsername = normalizeUsername(adminUsername);
        this.adminPassword = clean(adminPassword);
    }

    /**
     * 确保管理员账号存在。
     * <p>
     * 应用启动时调用，若数据库中不存在预设管理员账号则自动创建。
     * 管理员用户名和密码来自配置文件，若未配置则跳过。
     * </p>
     */
    /**
     * 确保管理员账号存在。
     * <p>
     * 应用启动时调用，若数据库中不存在预设管理员账号则自动创建。
     * 管理员用户名和密码来自配置文件，若未配置则跳过。
     * </p>
     */
    /**
     * 确保管理员账号存在。
     * <p>
     * 应用启动时调用，若数据库中不存在预设管理员账号则自动创建。
     * 管理员用户名和密码来自配置文件，若未配置则跳过。
     * </p>
     */
    @Transactional
    public void ensureAdminAccount() {
        if (adminUsername.isEmpty() || adminPassword.isEmpty()) {
            return;
        }
        userAccountRepository.findByUsername(adminUsername).ifPresentOrElse(
                it -> {
                },
                () -> {
                    UserAccount admin = new UserAccount();
                    admin.setUsername(adminUsername);
                    admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                    admin.setRole(ROLE_ADMIN);
                    admin.setVip(true);
                    admin.setBlacklisted(false);
                    admin.setRegisterIp("127.0.0.1");
                    admin.setRegisterDate(LocalDate.now(zoneId));
                    admin.setCreatedAt(Instant.now());
                    userAccountRepository.save(admin);
                }
        );
    }

    /**
     * 用户注册。
     * <p>
     * 校验用户名密码格式，检查同 IP 当日注册次数限制，
     * 创建用户账号并生成登录会话。
     * </p>
     *
     * @param request  包含用户名和密码的注册请求
     * @param clientIp 客户端 IP 地址
     * @return 包含 Token 和用户信息的认证响应
     */
    @Transactional
    public AuthResponse register(AuthRequest request, String clientIp) {
        String username = normalizeUsername(request.getUsername());
        String password = clean(request.getPassword());
        validateCredential(username, password);
        String ip = normalizeIp(clientIp);

        // 查询或初始化当日该 IP 的注册计数记录
        LocalDate today = LocalDate.now(zoneId);
        RegisterIpDaily ipDaily = registerIpDailyRepository
                .findByIpAndDayDate(ip, today)
                .orElseGet(() -> {
                    RegisterIpDaily row = new RegisterIpDaily();
                    row.setIp(ip);
                    row.setDayDate(today);
                    row.setRegisterCount(0);
                    row.setUpdatedAt(Instant.now());
                    return row;
                });

        // 同 IP 当日注册次数超限则拒绝
        if (ipDaily.getRegisterCount() >= maxRegisterPerIpPerDay) {
            throw new IllegalArgumentException("registration limit reached for this IP today");
        }
        // 用户名唯一性校验
        if (userAccountRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("username already exists");
        }

        // 创建新用户账号
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(ROLE_USER);
        user.setVip(false);
        user.setBlacklisted(false);
        user.setRegisterIp(ip);
        user.setRegisterDate(today);
        user.setCreatedAt(Instant.now());
        user = userAccountRepository.save(user);

        // 更新该 IP 当日注册计数
        ipDaily.setRegisterCount(ipDaily.getRegisterCount() + 1);
        ipDaily.setUpdatedAt(Instant.now());
        registerIpDailyRepository.save(ipDaily);

        return createSession(user);
    }

    /**
     * 用户登录。
     * <p>
     * 校验用户名密码，验证账号未被拉黑后创建新会话。
     * </p>
     *
     * @param request 包含用户名和密码的登录请求
     * @return 包含 Token 和用户信息的认证响应
     */
    /**
     * 用户登录。
     * <p>
     * 校验用户名密码，验证账号未被拉黑后创建新会话。
     * </p>
     *
     * @param request 包含用户名和密码的登录请求
     * @return 包含 Token 和用户信息的认证响应
     */
    @Transactional
    public AuthResponse login(AuthRequest request) {
        String username = normalizeUsername(request.getUsername());
        String password = clean(request.getPassword());
        validateCredential(username, password);

        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("invalid username or password"));
        // 密码校验
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("invalid username or password");
        }
        // 黑名单校验
        if (Boolean.TRUE.equals(user.getBlacklisted())) {
            throw new ForbiddenException("account is blacklisted");
        }
        // 更新最后登录时间
        user.setLastLoginAt(Instant.now());
        userAccountRepository.save(user);
        return createSession(user);
    }

    /**
     * 用户登出，删除当前会话。
     *
     * @param request HTTP 请求，从中提取 Token
     */
    /**
     * 用户登出，删除当前会话。
     *
     * @param request HTTP 请求，从中提取 Token
     */
    /**
     * 用户登出，删除当前会话。
     *
     * @param request HTTP 请求，从中提取 Token
     */
    /**
     * 用户登出，删除当前会话。
     *
     * @param request HTTP 请求，从中提取 Token
     */
    /**
     * 用户登出，删除当前会话。
     *
     * @param request HTTP 请求，从中提取 Token
     */
    @Transactional
    public void logout(HttpServletRequest request) {
        String token = parseToken(request);
        if (token.isEmpty()) {
            return;
        }
        userSessionRepository.deleteByToken(token);
    }

    /**
     * 根据 Token 获取当前登录用户，未登录或 Token 无效则抛出异常。
     * <p>
     * 同时校验用户是否被拉黑。
     * </p>
     *
     * @param request HTTP 请求
     * @return 当前登录的用户账号
     */
    @Transactional(readOnly = true)
    public UserAccount requireUser(HttpServletRequest request) {
        String token = parseToken(request);
        if (token.isEmpty()) {
            throw new UnauthorizedException("missing auth token");
        }
        UserSession session = userSessionRepository.findByTokenAndExpiresAtAfter(token, Instant.now())
                .orElseThrow(() -> new UnauthorizedException("token expired or invalid"));
        UserAccount user = userAccountRepository.findById(session.getUserId())
                .orElseThrow(() -> new UnauthorizedException("user not found"));
        if (Boolean.TRUE.equals(user.getBlacklisted())) {
            throw new ForbiddenException("account is blacklisted");
        }
        return user;
    }

    /**
     * 要求当前用户为管理员，否则抛出权限异常。
     *
     * @param request HTTP 请求
     * @return 管理员用户账号
     */
    /**
     * 要求当前用户为管理员，否则抛出权限异常。
     *
     * @param request HTTP 请求
     * @return 管理员用户账号
     */
    /**
     * 要求当前用户为管理员，否则抛出权限异常。
     *
     * @param request HTTP 请求
     * @return 管理员用户账号
     */
    @Transactional(readOnly = true)
    public UserAccount requireAdmin(HttpServletRequest request) {
        UserAccount user = requireUser(request);
        if (!ROLE_ADMIN.equalsIgnoreCase(clean(user.getRole()))) {
            throw new ForbiddenException("admin access required");
        }
        return user;
    }

    /**
     * 将用户账号实体转换为前端用户信息响应 DTO。
     *
     * @param user 用户账号实体
     * @return 用户信息响应对象
     */
    /**
     * 将用户账号实体转换为前端用户信息响应 DTO。
     *
     * @param user 用户账号实体
     * @return 用户信息响应对象
     */
    /**
     * 将用户账号实体转换为前端用户信息响应 DTO。
     *
     * @param user 用户账号实体
     * @return 用户信息响应对象
     */
    /**
     * 将用户账号实体转换为前端用户信息响应 DTO。
     *
     * @param user 用户账号实体
     * @return 用户信息响应对象
     */
    /**
     * 将用户账号实体转换为前端用户信息响应 DTO。
     *
     * @param user 用户账号实体
     * @return 用户信息响应对象
     */
    public UserInfoResponse toUserInfo(UserAccount user) {
        UserInfoResponse info = new UserInfoResponse();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setRole(user.getRole());
        info.setVip(Boolean.TRUE.equals(user.getVip()));
        info.setBlacklisted(Boolean.TRUE.equals(user.getBlacklisted()));
        return info;
    }

    private AuthResponse createSession(UserAccount user) {
        userSessionRepository.deleteByExpiresAtBefore(Instant.now());

        UserSession session = new UserSession();
        session.setUserId(user.getId());
        session.setToken(generateToken());
        session.setCreatedAt(Instant.now());
        session.setLastSeenAt(Instant.now());
        session.setExpiresAt(Instant.now().plusSeconds(tokenExpireHours * 3600));
        session = userSessionRepository.save(session);

        AuthResponse response = new AuthResponse();
        response.setToken(session.getToken());
        response.setExpiresAt(session.getExpiresAt());
        response.setUser(toUserInfo(user));
        return response;
    }

    private String generateToken() {
        byte[] bytes = new byte[36];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void validateCredential(String username, String password) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("username format invalid, use 4-32 letters/numbers/_");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("password must be at least 8 characters");
        }
    }

    public String getClientIp(HttpServletRequest request) {
        String forwarded = clean(request.getHeader("X-Forwarded-For"));
        if (!forwarded.isEmpty()) {
            int comma = forwarded.indexOf(',');
            if (comma > 0) {
                return normalizeIp(forwarded.substring(0, comma));
            }
            return normalizeIp(forwarded);
        }
        return normalizeIp(request.getRemoteAddr());
    }

    private String parseToken(HttpServletRequest request) {
        String auth = clean(request.getHeader("Authorization"));
        if (auth.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            return auth.substring(7).trim();
        }
        return clean(request.getHeader("X-Auth-Token"));
    }

    private String normalizeUsername(String username) {
        return clean(username).toLowerCase(Locale.ROOT);
    }

    private String normalizeIp(String ip) {
        String value = clean(ip);
        if (value.isEmpty()) {
            return "unknown";
        }
        if (value.length() > 64) {
            return value.substring(0, 64);
        }
        return value;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
