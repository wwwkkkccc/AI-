package com.resumeai.repository;

import com.resumeai.model.AnalysisRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRecordRepository extends JpaRepository<AnalysisRecord, Long> {
    Page<AnalysisRecord> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
    Page<AnalysisRecord> findAllByOrderByIdDesc(Pageable pageable);
    Page<AnalysisRecord> findByUsernameContainingIgnoreCaseOrderByIdDesc(String username, Pageable pageable);
}
