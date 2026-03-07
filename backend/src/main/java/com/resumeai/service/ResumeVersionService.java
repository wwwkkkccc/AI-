package com.resumeai.service;

import com.resumeai.dto.ResumeVersionDetail;
import com.resumeai.dto.ResumeVersionItem;
import com.resumeai.dto.ResumeVersionResponse;
import com.resumeai.dto.VersionCompareResponse;
import com.resumeai.model.ResumeVersion;
import com.resumeai.repository.ResumeVersionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 简历版本管理服务
 */
@Service
public class ResumeVersionService {

    private final ResumeVersionRepository resumeVersionRepository;

    public ResumeVersionService(ResumeVersionRepository resumeVersionRepository) {
        this.resumeVersionRepository = resumeVersionRepository;
    }

    @Transactional
    public ResumeVersion saveVersion(Long userId, String title, String content, String targetRole, String sourceType, Long sourceId) {
        String cleanTitle = clean(title);
        String cleanContent = clean(content);

        if (cleanTitle.isEmpty()) {
            throw new IllegalArgumentException("title is required");
        }
        if (cleanContent.isEmpty()) {
            throw new IllegalArgumentException("content is required");
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

    @Transactional(readOnly = true)
    public ResumeVersionResponse listVersions(Long userId, int page, int size) {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 100) {
            size = 20;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ResumeVersion> pageResult = resumeVersionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<ResumeVersionItem> items = new ArrayList<>();
        for (ResumeVersion version : pageResult.getContent()) {
            ResumeVersionItem item = new ResumeVersionItem();
            item.setId(version.getId());
            item.setTitle(version.getTitle());
            item.setTargetRole(version.getTargetRole());
            item.setSourceType(version.getSourceType());
            item.setSourceId(version.getSourceId());
            item.setCreatedAt(version.getCreatedAt());
            item.setUpdatedAt(version.getUpdatedAt());
            items.add(item);
        }

        ResumeVersionResponse response = new ResumeVersionResponse();
        response.setItems(items);
        response.setPage(page);
        response.setSize(size);
        response.setTotal(pageResult.getTotalElements());
        return response;
    }

    @Transactional(readOnly = true)
    public ResumeVersionDetail getVersion(Long id, Long userId) {
        ResumeVersion version = resumeVersionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("version not found"));

        ResumeVersionDetail detail = new ResumeVersionDetail();
        detail.setId(version.getId());
        detail.setTitle(version.getTitle());
        detail.setContent(version.getContent());
        detail.setTargetRole(version.getTargetRole());
        detail.setSourceType(version.getSourceType());
        detail.setSourceId(version.getSourceId());
        detail.setCreatedAt(version.getCreatedAt());
        detail.setUpdatedAt(version.getUpdatedAt());
        return detail;
    }

    @Transactional
    public void deleteVersion(Long id, Long userId) {
        if (!resumeVersionRepository.findByIdAndUserId(id, userId).isPresent()) {
            throw new IllegalArgumentException("version not found");
        }
        resumeVersionRepository.deleteByIdAndUserId(id, userId);
    }

    @Transactional(readOnly = true)
    public VersionCompareResponse compareVersions(Long id1, Long id2, Long userId) {
        ResumeVersion v1 = resumeVersionRepository.findByIdAndUserId(id1, userId)
                .orElseThrow(() -> new IllegalArgumentException("version 1 not found"));
        ResumeVersion v2 = resumeVersionRepository.findByIdAndUserId(id2, userId)
                .orElseThrow(() -> new IllegalArgumentException("version 2 not found"));

        List<VersionCompareResponse.DiffLine> diffLines = computeDiff(v1.getContent(), v2.getContent());

        VersionCompareResponse response = new VersionCompareResponse();
        response.setId1(v1.getId());
        response.setTitle1(v1.getTitle());
        response.setId2(v2.getId());
        response.setTitle2(v2.getTitle());
        response.setLines(diffLines);
        return response;
    }

    private List<VersionCompareResponse.DiffLine> computeDiff(String content1, String content2) {
        String[] lines1 = content1.split("\\n");
        String[] lines2 = content2.split("\\n");

        List<VersionCompareResponse.DiffLine> result = new ArrayList<>();

        // 简单实现：逐行对比
        int i = 0, j = 0;
        while (i < lines1.length || j < lines2.length) {
            if (i >= lines1.length) {
                // v1 已结束，v2 剩余的都是新增
                result.add(new VersionCompareResponse.DiffLine("ADDED", lines2[j]));
                j++;
            } else if (j >= lines2.length) {
                // v2 已结束，v1 剩余的都是删除
                result.add(new VersionCompareResponse.DiffLine("REMOVED", lines1[i]));
                i++;
            } else if (lines1[i].equals(lines2[j])) {
                // 相同行
                result.add(new VersionCompareResponse.DiffLine("SAME", lines1[i]));
                i++;
                j++;
            } else {
                // 不同行，简单处理：标记为删除和新增
                result.add(new VersionCompareResponse.DiffLine("REMOVED", lines1[i]));
                result.add(new VersionCompareResponse.DiffLine("ADDED", lines2[j]));
                i++;
                j++;
            }
        }

        return result;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
