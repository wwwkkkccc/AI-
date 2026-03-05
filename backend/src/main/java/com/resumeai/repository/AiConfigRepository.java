package com.resumeai.repository;

import com.resumeai.model.AiConfig;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * AI配置数据访问接口，提供ai_config表的基本CRUD操作
 */
public interface AiConfigRepository extends JpaRepository<AiConfig, Long> {
}
