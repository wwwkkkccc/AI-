package com.resumeai.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AuthBootstrap implements CommandLineRunner {
    private final AuthService authService;

    public AuthBootstrap(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        authService.ensureAdminAccount();
    }
}
