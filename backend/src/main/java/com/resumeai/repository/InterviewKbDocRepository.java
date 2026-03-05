package com.resumeai.repository;

import com.resumeai.model.InterviewKbDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewKbDocRepository extends JpaRepository<InterviewKbDoc, Long> {
    Page<InterviewKbDoc> findAllByOrderByIdDesc(Pageable pageable);
}
