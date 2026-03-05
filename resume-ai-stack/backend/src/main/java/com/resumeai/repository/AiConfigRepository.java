package com.resumeai.repository;

import com.resumeai.model.AiConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConfigRepository extends JpaRepository<AiConfig, Long> {
}
