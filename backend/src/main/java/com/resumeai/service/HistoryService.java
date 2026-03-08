package com.resumeai.service;

import com.resumeai.dto.AnalysisHistoryItem;
import com.resumeai.dto.AnalysisHistoryResponse;
import com.resumeai.model.AnalysisRecord;
import com.resumeai.repository.AnalysisHistoryListProjection;
import com.resumeai.repository.AnalysisRecordRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Query service for analysis history list/detail views.
 */
@Service
public class HistoryService {
    private final AnalysisRecordRepository analysisRecordRepository;

    public HistoryService(AnalysisRecordRepository analysisRecordRepository) {
        this.analysisRecordRepository = analysisRecordRepository;
    }

    /** Returns paged history summaries for the current user. */
    @Transactional(readOnly = true)
    public AnalysisHistoryResponse listMine(Long userId, int page, int size) {
        Pageable pageable = normalizePage(page, size);
        Page<AnalysisHistoryListProjection> result = analysisRecordRepository.pageSummaryByUserId(userId, pageable);
        return toSummaryResponse(result, pageable);
    }

    /** Returns paged history summaries for admin with optional username keyword filter. */
    @Transactional(readOnly = true)
    public AnalysisHistoryResponse listAllForAdmin(String usernameLike, int page, int size) {
        Pageable pageable = normalizePage(page, size);
        String keyword = usernameLike == null ? "" : usernameLike.trim();
        Page<AnalysisHistoryListProjection> result = analysisRecordRepository.pageSummaryForAdmin(keyword, pageable);
        return toSummaryResponse(result, pageable);
    }

    /** Returns one full analysis record detail for admin usage. */
    @Transactional(readOnly = true)
    public AnalysisHistoryItem getDetailForAdmin(Long id) {
        AnalysisRecord record = analysisRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("analysis record not found"));
        return toItem(record, true);
    }

    private AnalysisHistoryResponse toSummaryResponse(Page<AnalysisHistoryListProjection> pageResult, Pageable pageable) {
        List<AnalysisHistoryItem> items = pageResult.getContent().stream()
                .map(this::toSummaryItem)
                .toList();

        AnalysisHistoryResponse response = new AnalysisHistoryResponse();
        response.setItems(items);
        response.setTotal(pageResult.getTotalElements());
        response.setPage(pageable.getPageNumber());
        response.setSize(pageable.getPageSize());
        return response;
    }

    private AnalysisHistoryResponse toResponse(Page<AnalysisRecord> pageResult, Pageable pageable, boolean includeFullText) {
        List<AnalysisHistoryItem> items = pageResult.getContent().stream()
                .map(record -> toItem(record, includeFullText))
                .toList();

        AnalysisHistoryResponse response = new AnalysisHistoryResponse();
        response.setItems(items);
        response.setTotal(pageResult.getTotalElements());
        response.setPage(pageable.getPageNumber());
        response.setSize(pageable.getPageSize());
        return response;
    }

    private AnalysisHistoryItem toSummaryItem(AnalysisHistoryListProjection row) {
        AnalysisHistoryItem item = new AnalysisHistoryItem();
        item.setId(row.getId());
        item.setUserId(row.getUserId());
        item.setUsername(row.getUsername());
        item.setFilename(row.getFilename());
        item.setTargetRole(row.getTargetRole());
        item.setScore(row.getScore());
        item.setCoverage(row.getCoverage());
        item.setOptimizedSummary(row.getOptimizedSummary());
        item.setModelUsed(Boolean.TRUE.equals(row.getModelUsed()));
        item.setResumePreview(cleanPreview(row.getResumePreview(), 300));
        item.setJdPreview(cleanPreview(row.getJdPreview(), 240));
        item.setCreatedAt(row.getCreatedAt());
        return item;
    }

    private AnalysisHistoryItem toItem(AnalysisRecord record, boolean includeFullText) {
        AnalysisHistoryItem item = new AnalysisHistoryItem();
        item.setId(record.getId());
        item.setUserId(record.getUserId());
        item.setUsername(record.getUsername());
        item.setFilename(record.getFilename());
        item.setTargetRole(record.getTargetRole());
        item.setScore(record.getScore());
        item.setCoverage(record.getCoverage());
        item.setOptimizedSummary(record.getOptimizedSummary());
        item.setModelUsed(Boolean.TRUE.equals(record.getModelUsed()));
        item.setResumePreview(preview(record.getResumeText(), 300));
        item.setJdPreview(preview(record.getJdText(), 240));
        if (includeFullText) {
            item.setResumeText(record.getResumeText());
            item.setJdText(record.getJdText());
        }
        item.setCreatedAt(record.getCreatedAt());
        return item;
    }

    private String preview(String text, int max) {
        if (text == null) {
            return "";
        }
        String clean = cleanPreview(text, max);
        if (clean.length() <= max) {
            return clean;
        }
        return clean.substring(0, max) + "...";
    }

    private String cleanPreview(String text, int max) {
        if (text == null) {
            return "";
        }
        String clean = text.replace("\r", " ").replace("\n", " ").trim();
        if (clean.length() > max) {
            return clean.substring(0, max);
        }
        return clean;
    }

    private Pageable normalizePage(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(50, Math.max(size, 1));
        return PageRequest.of(safePage, safeSize);
    }
}
