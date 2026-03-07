package com.resumeai.repository;

import com.resumeai.model.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Resume version repository.
 */
public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {
}

