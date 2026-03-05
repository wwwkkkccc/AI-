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

    @Transactional(readOnly = true)
    public InterviewKbDocQuestionsResponse listQuestions(Long docId, int page, int size) {
        InterviewKbDoc doc = docRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("question doc not found"));

        Pageable pageable = normalizePage(page, size);
        Page<InterviewKbItem> itemPage = itemRepository.findByDocIdOrderByIdAsc(docId, pageable);
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

    private Pageable normalizePage(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(200, Math.max(1, size));
        return PageRequest.of(safePage, safeSize);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
