package com.resumeai.repository;

import com.resumeai.model.InterviewKbItem;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewKbItemRepository extends JpaRepository<InterviewKbItem, Long> {
    List<InterviewKbItem> findTop3000ByOrderByIdDesc();

    Page<InterviewKbItem> findByDocIdOrderByIdAsc(Long docId, Pageable pageable);

    void deleteByDocId(Long docId);
}
