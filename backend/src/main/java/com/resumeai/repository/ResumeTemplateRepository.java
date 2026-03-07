package com.resumeai.repository;

import com.resumeai.model.ResumeTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 简历模板数据访问接口
 */
public interface ResumeTemplateRepository extends JpaRepository<ResumeTemplate, Long> {
    Page<ResumeTemplate> findAllByOrderByUsageCountDesc(Pageable pageable);
    Page<ResumeTemplate> findByIndustryOrderByUsageCountDesc(String industry, Pageable pageable);
}
