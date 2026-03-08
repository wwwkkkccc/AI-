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
 * Admin-side user management service: paged listing and status updates.
 */
@Service
public class UserManageService {
    private final UserAccountRepository userAccountRepository;

    public UserManageService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    /** Returns paged users, optionally filtered by username keyword. */
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

    /** Updates VIP/blacklist state for one user; admin account cannot be blacklisted. */
    @Transactional
    public AdminUserItem updateUserStatus(Long userId, AdminUserUpdateRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        if (request.getVip() != null) {
            user.setVip(request.getVip());
        }
        if (request.getBlacklisted() != null) {
            if ("ADMIN".equalsIgnoreCase(user.getRole()) && request.getBlacklisted()) {
                throw new IllegalArgumentException("cannot blacklist admin");
            }
            user.setBlacklisted(request.getBlacklisted());
        }
        user = userAccountRepository.save(user);
        return toItem(user);
    }

    /** Converts entity to admin list DTO. */
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

    /** Normalizes paging params and guards against oversized requests. */
    private Pageable normalizePage(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(100, Math.max(size, 1));
        return PageRequest.of(safePage, safeSize);
    }
}
