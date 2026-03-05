package com.resumeai.service;

import com.resumeai.dto.AdminUserItem;
import com.resumeai.dto.AdminUserUpdateRequest;
import com.resumeai.dto.AdminUsersResponse;
import com.resumeai.model.UserAccount;
import com.resumeai.repository.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户管理服务（管理员专用）。
 * 提供用户列表查询（支持关键字搜索）以及用户状态更新（VIP、黑名单）功能。
 */
@Service
public class UserManageService {
    private final UserAccountRepository userAccountRepository;

    public UserManageService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * 分页查询用户列表。
     * 支持按用户名关键字模糊搜索，传 null 或空串则返回全部用户。
     */
    @Transactional(readOnly = true)
    public AdminUsersResponse listUsers(String keyword, int page, int size) {
        Pageable pageable = normalizePage(page, size);
        Page<UserAccount> results;
        if (keyword == null || keyword.isBlank()) {
            results = userAccountRepository.findAllByOrderByIdDesc(pageable);
        } else {
            results = userAccountRepository.findByUsernameContainingIgnoreCaseOrderByIdDesc(keyword.trim(), pageable);
        }

        AdminUsersResponse response = new AdminUsersResponse();
        response.setItems(results.getContent().stream().map(this::toItem).toList());
        response.setTotal(results.getTotalElements());
        response.setPage(pageable.getPageNumber());
        response.setSize(pageable.getPageSize());
        return response;
    }

    /**
     * 更新指定用户的状态（VIP / 黑名单）。
     * 管理员账号不允许被加入黑名单。
     */
    @Transactional
    public AdminUserItem updateUserStatus(Long userId, AdminUserUpdateRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        if (request.getVip() != null) {
            user.setVip(request.getVip());
        }
        if (request.getBlacklisted() != null) {
            // 禁止将管理员加入黑名单
            if ("ADMIN".equalsIgnoreCase(user.getRole()) && request.getBlacklisted()) {
                throw new IllegalArgumentException("cannot blacklist admin");
            }
            user.setBlacklisted(request.getBlacklisted());
        }
        user = userAccountRepository.save(user);
        return toItem(user);
    }

    /** 将用户实体转换为管理端列表项 DTO */
    private AdminUserItem toItem(UserAccount user) {
        AdminUserItem item = new AdminUserItem();
        item.setId(user.getId());
        item.setUsername(user.getUsername());
        item.setRole(user.getRole());
        item.setVip(Boolean.TRUE.equals(user.getVip()));
        item.setBlacklisted(Boolean.TRUE.equals(user.getBlacklisted()));
        item.setCreatedAt(user.getCreatedAt());
        item.setLastLoginAt(user.getLastLoginAt());
        return item;
    }

    /** 规范化分页参数，防止越界 */
    private Pageable normalizePage(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(100, Math.max(size, 1));
        return PageRequest.of(safePage, safeSize);
    }
}
