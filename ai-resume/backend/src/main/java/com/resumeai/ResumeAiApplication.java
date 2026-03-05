package com.resumeai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI 简历分析系统的 Spring Boot 启动类。
 * 开启组件扫描、自动配置以及定时任务调度。
 */
@SpringBootApplication
@EnableScheduling
public class ResumeAiApplication {
    /** 应用程序入口方法 */
    public static void main(String[] args) {
        SpringApplication.run(ResumeAiApplication.class, args);
    }
}
