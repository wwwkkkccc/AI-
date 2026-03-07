package com.resumeai.repository;

import com.resumeai.model.AnalysisJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 分析任务数据访问接口，提供analysis_jobs表的查询操作
 */
public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, String> {
    // 根据任务状态查询任务列表
    List<AnalysisJob> findByStatus(String status);

    // 根据批次ID查询任务列表
    List<AnalysisJob> findByBatchId(String batchId);
}
