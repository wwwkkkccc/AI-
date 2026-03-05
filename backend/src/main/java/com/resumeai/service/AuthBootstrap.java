package com.resumeai.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动引导器。
 * 在 Spring Boot 启动完成后自动执行，确保系统中存在默认管理员账号。
 */
@Component
public class AuthBootstrap implements CommandLineRunner {
    private final AuthService authService;

    public AuthBootstrap(AuthService authService) {
        this.authService = authService;
    }

    /** 启动时自动调用，初始化管理员账号 */
    @Override
    public void run(String... args) {
        authService.ensureAdminAccount();
    }
}
