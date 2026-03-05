package com.resumeai.service;

import com.resumeai.dto.AnalysisHistoryItem;
import com.resumeai.dto.AnalysisHistoryResponse;
import com.resumeai.model.AnalysisRecord;
import com.resumeai.repository.AnalysisRecordRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryService {
    private final AnalysisRecordRepository analysisRecordRepository;

    public HistoryService(AnalysisRecordRepository analysisRecordRepository) {
        this.analysisRecordRepository = analysisRecordRepository;
    }

    @Transactional(readOnly = true)
    public AnalysisHistoryResponse listMine(Long userId, int page, int size) {
        Pageable pageable = normalizePage(page, size);
        Page<AnalysisRecord> result = analysisRecordRepository.findByUserIdOrderByIdDesc(userId, pageable);
        return toResponse(result, pageable);
    }

    @Transactional(readOnly = true)
    public AnalysisHistoryResponse listAllForAdmin(String usernameLike, int page, int size) {
        Pageable pageable = normalizePage(page, size);
        Page<AnalysisRecord> result;
        if (usernameLike == null || usernameLike.isBlank()) {
            result = analysisRecordRepository.findAllByOrderByIdDesc(pageable);
        } else {
            result = analysisRecordRepository.findByUsernameContainingIgnoreCaseOrderByIdDesc(usernameLike.trim(), pageable);
        }
        return toResponse(result, pageable);
    }

    private AnalysisHistoryResponse toResponse(Page<AnalysisRecord> pageResult, Pageable pageable) {
        List<AnalysisHistoryItem> items = pageResult.getContent().stream().map(this::toItem).toList();
        AnalysisHistoryResponse response = new AnalysisHistoryResponse();
        response.setItems(items);
        response.setTotal(pageResult.getTotalElements());
        response.setPage(pageable.getPageNumber());
        response.setSize(pageable.getPageSize());
        return response;
    }

    private AnalysisHistoryItem toItem(AnalysisRecord record) {
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
        item.setResumeText(record.getResumeText());
        item.setJdText(record.getJdText());
        item.setCreatedAt(record.getCreatedAt());
        return item;
    }

    private String preview(String text, int max) {
        if (text == null) {
            return "";
        }
        String clean = text.replace("\r", " ").replace("\n", " ").trim();
        if (clean.length() <= max) {
            return clean;
        }
        return clean.substring(0, max) + "...";
    }

    private Pageable normalizePage(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(50, Math.max(size, 1));
        return PageRequest.of(safePage, safeSize);
    }
}
