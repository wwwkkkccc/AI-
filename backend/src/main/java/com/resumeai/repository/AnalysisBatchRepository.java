package com.resumeai.repository;

import com.resumeai.model.AnalysisBatch;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisBatchRepository extends JpaRepository<AnalysisBatch, String> {
    Optional<AnalysisBatch> findByIdAndUserId(String id, Long userId);
    Page<AnalysisBatch> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
