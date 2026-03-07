package com.resumeai.repository;

import java.time.Instant;

public interface AnalysisHistoryListProjection {
    Long getId();

    Long getUserId();

    String getUsername();

    String getFilename();

    String getTargetRole();

    Double getScore();

    Double getCoverage();

    String getOptimizedSummary();

    Boolean getModelUsed();

    String getResumePreview();

    String getJdPreview();

    Instant getCreatedAt();
}
