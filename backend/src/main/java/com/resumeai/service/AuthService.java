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
 * Authentication service for registration, login/logout, session validation, and role checks.
 */
@Service
public class AuthService {
    /** Username format: 4-32 characters, letters/numbers/underscore only. */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,32}$");
    /** Administrator role marker. */
    private static final String ROLE_ADMIN = "ADMIN";
    /** Standard user role marker. */
    private static final String ROLE_USER = "USER";

    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final RegisterIpDailyRepository registerIpDailyRepository;
    /** BCrypt password hasher and verifier. */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    /** Secure random source for token generation. */
    private final SecureRandom secureRandom = new SecureRandom();
    /** Business timezone used by per-day registration limit. */
    private final ZoneId zoneId;
    /** Token expiration in hours. */
    private final long tokenExpireHours;
    /** Max registrations allowed from one IP per day. */
    private final int maxRegisterPerIpPerDay;
    /** Bootstrap admin username from config. */
    private final String adminUsername;
    /** Bootstrap admin password from config. */
    private final String adminPassword;

    /** Builds the auth service and normalizes all auth-related runtime settings. */
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

    /** Ensures the bootstrap admin account exists when startup credentials are configured. */
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

    /** Registers a new user with per-IP daily limit and returns an authenticated session. */
    @Transactional
    public AuthResponse register(AuthRequest request, String clientIp) {
        String username = normalizeUsername(request.getUsername());
        String password = clean(request.getPassword());
        validateCredential(username, password);
        String ip = normalizeIp(clientIp);

        // Load or initialize the per-IP daily counter for the current date.
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

        // Enforce per-IP daily registration quota.
        if (ipDaily.getRegisterCount() >= maxRegisterPerIpPerDay) {
            throw new IllegalArgumentException("registration limit reached for this IP today");
        }
        // Enforce unique username.
        if (userAccountRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("username already exists");
        }

        // Create and persist the new user.
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

        // Persist daily IP usage.
        ipDaily.setRegisterCount(ipDaily.getRegisterCount() + 1);
        ipDaily.setUpdatedAt(Instant.now());
        registerIpDailyRepository.save(ipDaily);

        return createSession(user);
    }

    /** Authenticates a user, checks blacklist status, and creates a fresh session token. */
    @Transactional
    public AuthResponse login(AuthRequest request) {
        String username = normalizeUsername(request.getUsername());
        String password = clean(request.getPassword());
        validateCredential(username, password);

        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("invalid username or password"));
        // Verify password hash.
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("invalid username or password");
        }
        // Block blacklisted accounts.
        if (Boolean.TRUE.equals(user.getBlacklisted())) {
            throw new ForbiddenException("account is blacklisted");
        }
        // Update last login timestamp.
        user.setLastLoginAt(Instant.now());
        userAccountRepository.save(user);
        return createSession(user);
    }

    /** Logs out current user by deleting the token-bound session row. */
    @Transactional
    public void logout(HttpServletRequest request) {
        String token = parseToken(request);
        if (token.isEmpty()) {
            return;
        }
        userSessionRepository.deleteByToken(token);
    }

    /** Resolves and validates current user from bearer token, then enforces not-blacklisted. */
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

    /** Same as {@link #requireUser(HttpServletRequest)} but also enforces ADMIN role. */
    @Transactional(readOnly = true)
    public UserAccount requireAdmin(HttpServletRequest request) {
        UserAccount user = requireUser(request);
        if (!ROLE_ADMIN.equalsIgnoreCase(clean(user.getRole()))) {
            throw new ForbiddenException("admin access required");
        }
        return user;
    }

    /** Maps {@link UserAccount} to response DTO used by auth endpoints. */
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

    /** Extracts client IP from reverse-proxy header first, then fallback to remote address. */
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

    /** Supports both `Authorization: Bearer` and `X-Auth-Token` headers. */
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
