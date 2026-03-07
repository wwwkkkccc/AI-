package com.resumeai.repository;

import com.resumeai.model.ResumeVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 简历版本数据访问接口
 */
public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {
    Page<ResumeVersion> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<ResumeVersion> findByIdAndUserId(Long id, Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}
