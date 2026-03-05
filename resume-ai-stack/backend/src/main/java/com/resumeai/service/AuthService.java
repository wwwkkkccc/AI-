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

@Service
public class AuthService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,32}$");
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";

    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final RegisterIpDailyRepository registerIpDailyRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();
    private final ZoneId zoneId;
    private final long tokenExpireHours;
    private final int maxRegisterPerIpPerDay;
    private final String adminUsername;
    private final String adminPassword;

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

    @Transactional
    public AuthResponse register(AuthRequest request, String clientIp) {
        String username = normalizeUsername(request.getUsername());
        String password = clean(request.getPassword());
        validateCredential(username, password);
        String ip = normalizeIp(clientIp);

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

        if (ipDaily.getRegisterCount() >= maxRegisterPerIpPerDay) {
            throw new IllegalArgumentException("registration limit reached for this IP today");
        }
        if (userAccountRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("username already exists");
        }

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

        ipDaily.setRegisterCount(ipDaily.getRegisterCount() + 1);
        ipDaily.setUpdatedAt(Instant.now());
        registerIpDailyRepository.save(ipDaily);

        return createSession(user);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        String username = normalizeUsername(request.getUsername());
        String password = clean(request.getPassword());
        validateCredential(username, password);

        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("invalid username or password"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("invalid username or password");
        }
        if (Boolean.TRUE.equals(user.getBlacklisted())) {
            throw new ForbiddenException("account is blacklisted");
        }
        user.setLastLoginAt(Instant.now());
        userAccountRepository.save(user);
        return createSession(user);
    }

    @Transactional
    public void logout(HttpServletRequest request) {
        String token = parseToken(request);
        if (token.isEmpty()) {
            return;
        }
        userSessionRepository.deleteByToken(token);
    }

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

    @Transactional(readOnly = true)
    public UserAccount requireAdmin(HttpServletRequest request) {
        UserAccount user = requireUser(request);
        if (!ROLE_ADMIN.equalsIgnoreCase(clean(user.getRole()))) {
            throw new ForbiddenException("admin access required");
        }
        return user;
    }

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
