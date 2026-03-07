package com.resumeai.repository;

import com.resumeai.model.InterviewKbCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 面试题库分类数据访问接口
 */
public interface InterviewKbCategoryRepository extends JpaRepository<InterviewKbCategory, Long> {
    List<InterviewKbCategory> findAllByOrderByIdAsc();

    List<InterviewKbCategory> findByParentIdOrderByIdAsc(Long parentId);
}
