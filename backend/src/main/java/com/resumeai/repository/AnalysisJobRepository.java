package com.resumeai.repository;

import com.resumeai.model.AnalysisJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, String> {
    List<AnalysisJob> findByStatus(String status);
}
