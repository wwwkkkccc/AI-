package com.resumeai.service;

import com.resumeai.dto.InterviewKbDocQuestionsResponse;
import com.resumeai.model.InterviewKbDoc;
import com.resumeai.model.InterviewKbItem;
import com.resumeai.repository.InterviewKbDocRepository;
import com.resumeai.repository.InterviewKbItemRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 面试题库文档查看服务。
 * 提供按文档 ID 分页查看其中面试题列表的能力。
 */
@Service
public class InterviewKbDocViewService {
    private final InterviewKbDocRepository docRepository;
    private final InterviewKbItemRepository itemRepository;

    public InterviewKbDocViewService(
            InterviewKbDocRepository docRepository,
            InterviewKbItemRepository itemRepository) {
        this.docRepository = docRepository;
        this.itemRepository = itemRepository;
    }

    /**
     * 分页查询指定文档下的面试题列表。
     * 若文档不存在则抛出异常。
     */
    @Transactional(readOnly = true)
    public InterviewKbDocQuestionsResponse listQuestions(Long docId, int page, int size) {
        InterviewKbDoc doc = docRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("question doc not found"));

        Pageable pageable = normalizePage(page, size);
        Page<InterviewKbItem> itemPage = itemRepository.findByDocIdOrderByIdAsc(docId, pageable);
        // 提取题目文本，过滤空值
        List<String> questions = itemPage.getContent().stream()
                .map(InterviewKbItem::getQuestionText)
                .map(this::clean)
                .filter(v -> !v.isEmpty())
                .toList();

        InterviewKbDocQuestionsResponse response = new InterviewKbDocQuestionsResponse();
        response.setDocId(doc.getId());
        response.setTitle(doc.getTitle());
        response.setFilename(doc.getFilename());
        response.setTotal(itemPage.getTotalElements());
        response.setPage(itemPage.getNumber());
        response.setSize(itemPage.getSize());
        response.setQuestions(questions);
        return response;
    }

    /** 规范化分页参数，防止越界 */
    private Pageable normalizePage(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(200, Math.max(1, size));
        return PageRequest.of(safePage, safeSize);
    }

    /** 清理字符串：null 转空串并去除首尾空白 */
    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
