package com.resumeai.service;

import com.resumeai.model.AuditLog;
import com.resumeai.model.UserAccount;
import com.resumeai.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 审计日志服务
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * 记录管理员操作日志
     */
    public void log(UserAccount admin, String action, String targetType, String targetId, String detail, String ip) {
        AuditLog log = new AuditLog();
        log.setAdminId(admin.getId());
        log.setAdminUsername(admin.getUsername());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setIpAddress(ip);
        log.setCreatedAt(Instant.now());
        auditLogRepository.save(log);
    }

    /**
     * 查询审计日志列表
     */
    public Page<AuditLog> listLogs(String action, String adminUsername, LocalDate from, LocalDate to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        if (action != null && !action.isBlank()) {
            return auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable);
        }

        if (adminUsername != null && !adminUsername.isBlank()) {
            return auditLogRepository.findByAdminUsernameContainingIgnoreCaseOrderByCreatedAtDesc(adminUsername, pageable);
        }

        if (from != null && to != null) {
            Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant toInstant = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            return auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(fromInstant, toInstant, pageable);
        }

        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
