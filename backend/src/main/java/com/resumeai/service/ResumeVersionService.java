package com.resumeai.service;

import com.resumeai.model.ResumeVersion;
import com.resumeai.repository.ResumeVersionRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists generated/rewritten resume snapshots for user history.
 */
@Service
public class ResumeVersionService {

    private final ResumeVersionRepository resumeVersionRepository;

    public ResumeVersionService(ResumeVersionRepository resumeVersionRepository) {
        this.resumeVersionRepository = resumeVersionRepository;
    }

    @Transactional
    public ResumeVersion saveVersion(
            Long userId,
            String title,
            String content,
            String targetRole,
            String sourceType,
            Long sourceId) {
        if (userId == null) {
            throw new IllegalArgumentException("user_id is required");
        }

        String cleanTitle = clean(title);
        String cleanContent = clean(content);
        if (cleanTitle.isEmpty()) {
            throw new IllegalArgumentException("title is required");
        }
        if (cleanContent.length() < 30) {
            throw new IllegalArgumentException("content is too short");
        }

        ResumeVersion version = new ResumeVersion();
        version.setUserId(userId);
        version.setTitle(cleanTitle);
        version.setContent(cleanContent);
        version.setTargetRole(clean(targetRole));
        version.setSourceType(clean(sourceType));
        version.setSourceId(sourceId);

        Instant now = Instant.now();
        version.setCreatedAt(now);
        version.setUpdatedAt(now);
        return resumeVersionRepository.save(version);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}

