package com.resumeai.service;

import com.resumeai.dto.InterviewKbCategoryResponse;
import com.resumeai.model.InterviewKbCategory;
import com.resumeai.model.InterviewKbItem;
import com.resumeai.repository.InterviewKbCategoryRepository;
import com.resumeai.repository.InterviewKbItemRepository;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 面试题库分类服务
 */
@Service
public class InterviewKbCategoryService {

    private static final Map<String, String> KEYWORD_CATEGORY_MAP = new HashMap<>();

    static {
        // 技术基础
        KEYWORD_CATEGORY_MAP.put("java", "技术基础");
        KEYWORD_CATEGORY_MAP.put("jvm", "技术基础");
        KEYWORD_CATEGORY_MAP.put("gc", "技术基础");
        KEYWORD_CATEGORY_MAP.put("多线程", "技术基础");
        KEYWORD_CATEGORY_MAP.put("线程", "技术基础");
        KEYWORD_CATEGORY_MAP.put("并发", "技术基础");
        KEYWORD_CATEGORY_MAP.put("锁", "技术基础");

        // 框架/中间件
        KEYWORD_CATEGORY_MAP.put("spring", "框架/中间件");
        KEYWORD_CATEGORY_MAP.put("mybatis", "框架/中间件");
        KEYWORD_CATEGORY_MAP.put("redis", "框架/中间件");
        KEYWORD_CATEGORY_MAP.put("kafka", "框架/中间件");
        KEYWORD_CATEGORY_MAP.put("rabbitmq", "框架/中间件");
        KEYWORD_CATEGORY_MAP.put("elasticsearch", "框架/中间件");
        KEYWORD_CATEGORY_MAP.put("dubbo", "框架/中间件");

        // 系统设计
        KEYWORD_CATEGORY_MAP.put("分布式", "系统设计");
        KEYWORD_CATEGORY_MAP.put("微服务", "系统设计");
        KEYWORD_CATEGORY_MAP.put("高并发", "系统设计");
        KEYWORD_CATEGORY_MAP.put("架构", "系统设计");
        KEYWORD_CATEGORY_MAP.put("设计模式", "系统设计");
        KEYWORD_CATEGORY_MAP.put("负载均衡", "系统设计");
        KEYWORD_CATEGORY_MAP.put("缓存", "系统设计");

        // 算法与数据结构
        KEYWORD_CATEGORY_MAP.put("算法", "算法与数据结构");
        KEYWORD_CATEGORY_MAP.put("数据结构", "算法与数据结构");
        KEYWORD_CATEGORY_MAP.put("排序", "算法与数据结构");
        KEYWORD_CATEGORY_MAP.put("查找", "算法与数据结构");
        KEYWORD_CATEGORY_MAP.put("树", "算法与数据结构");
        KEYWORD_CATEGORY_MAP.put("图", "算法与数据结构");
        KEYWORD_CATEGORY_MAP.put("链表", "算法与数据结构");

        // 行为面试
        KEYWORD_CATEGORY_MAP.put("沟通", "行为面试");
        KEYWORD_CATEGORY_MAP.put("团队", "行为面试");
        KEYWORD_CATEGORY_MAP.put("冲突", "行为面试");
        KEYWORD_CATEGORY_MAP.put("领导力", "行为面试");
        KEYWORD_CATEGORY_MAP.put("协作", "行为面试");
        KEYWORD_CATEGORY_MAP.put("管理", "行为面试");

        // 项目经验
        KEYWORD_CATEGORY_MAP.put("项目", "项目经验");
        KEYWORD_CATEGORY_MAP.put("难点", "项目经验");
        KEYWORD_CATEGORY_MAP.put("优化", "项目经验");
        KEYWORD_CATEGORY_MAP.put("性能", "项目经验");
        KEYWORD_CATEGORY_MAP.put("经验", "项目经验");
        KEYWORD_CATEGORY_MAP.put("挑战", "项目经验");

        // 数据库
        KEYWORD_CATEGORY_MAP.put("mysql", "数据库");
        KEYWORD_CATEGORY_MAP.put("sql", "数据库");
        KEYWORD_CATEGORY_MAP.put("索引", "数据库");
        KEYWORD_CATEGORY_MAP.put("事务", "数据库");
        KEYWORD_CATEGORY_MAP.put("数据库", "数据库");
        KEYWORD_CATEGORY_MAP.put("查询", "数据库");
        KEYWORD_CATEGORY_MAP.put("优化", "数据库");

        // 网络与安全
        KEYWORD_CATEGORY_MAP.put("网络", "网络与安全");
        KEYWORD_CATEGORY_MAP.put("tcp", "网络与安全");
        KEYWORD_CATEGORY_MAP.put("http", "网络与安全");
        KEYWORD_CATEGORY_MAP.put("https", "网络与安全");
        KEYWORD_CATEGORY_MAP.put("安全", "网络与安全");
        KEYWORD_CATEGORY_MAP.put("加密", "网络与安全");
        KEYWORD_CATEGORY_MAP.put("认证", "网络与安全");
    }

    private final InterviewKbCategoryRepository categoryRepository;
    private final InterviewKbItemRepository itemRepository;

    public InterviewKbCategoryService(
            InterviewKbCategoryRepository categoryRepository,
            InterviewKbItemRepository itemRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
    }

    @PostConstruct
    @Transactional
    public void initDefaultCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }

        String[] defaultCategories = {
                "技术基础",
                "框架/中间件",
                "系统设计",
                "算法与数据结构",
                "行为面试",
                "项目经验",
                "数据库",
                "网络与安全"
        };

        Instant now = Instant.now();
        for (String name : defaultCategories) {
            InterviewKbCategory category = new InterviewKbCategory();
            category.setName(name);
            category.setParentId(null);
            category.setQuestionCount(0);
            category.setCreatedAt(now);
            categoryRepository.save(category);
        }
    }

    @Transactional(readOnly = true)
    public List<InterviewKbCategoryResponse> listCategories() {
        List<InterviewKbCategory> all = categoryRepository.findAllByOrderByIdAsc();
        Map<Long, InterviewKbCategoryResponse> map = new HashMap<>();
        List<InterviewKbCategoryResponse> roots = new ArrayList<>();

        for (InterviewKbCategory category : all) {
            InterviewKbCategoryResponse response = toCategoryResponse(category);
            response.setChildren(new ArrayList<>());
            map.put(category.getId(), response);
        }

        for (InterviewKbCategory category : all) {
            InterviewKbCategoryResponse response = map.get(category.getId());
            if (category.getParentId() == null) {
                roots.add(response);
            } else {
                InterviewKbCategoryResponse parent = map.get(category.getParentId());
                if (parent != null) {
                    parent.getChildren().add(response);
                } else {
                    roots.add(response);
                }
            }
        }

        return roots;
    }

    @Transactional
    public InterviewKbCategoryResponse createCategory(String name, Long parentId) {
        String cleanName = clean(name);
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("category name is required");
        }
        if (cleanName.length() > 128) {
            throw new IllegalArgumentException("category name too long");
        }

        if (parentId != null) {
            categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("parent category not found"));
        }

        InterviewKbCategory category = new InterviewKbCategory();
        category.setName(cleanName);
        category.setParentId(parentId);
        category.setQuestionCount(0);
        category.setCreatedAt(Instant.now());
        category = categoryRepository.save(category);

        InterviewKbCategoryResponse response = toCategoryResponse(category);
        response.setChildren(new ArrayList<>());
        return response;
    }

    @Transactional
    public void deleteCategory(Long id) {
        InterviewKbCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("category not found"));

        List<InterviewKbCategory> children = categoryRepository.findByParentIdOrderByIdAsc(id);
        for (InterviewKbCategory child : children) {
            deleteCategory(child.getId());
        }

        List<InterviewKbItem> items = itemRepository.findAll().stream()
                .filter(item -> id.equals(item.getCategoryId()))
                .toList();
        for (InterviewKbItem item : items) {
            item.setCategoryId(null);
            itemRepository.save(item);
        }

        categoryRepository.delete(category);
    }

    @Transactional
    public Long autoCategorize(InterviewKbItem item) {
        if (item == null) {
            return null;
        }

        String text = clean(item.getQuestionText()).toLowerCase(Locale.ROOT);
        String keywords = clean(item.getKeywords()).toLowerCase(Locale.ROOT);
        String combined = text + " " + keywords;

        Map<String, Integer> categoryScores = new HashMap<>();

        for (Map.Entry<String, String> entry : KEYWORD_CATEGORY_MAP.entrySet()) {
            String keyword = entry.getKey();
            String categoryName = entry.getValue();

            if (combined.contains(keyword)) {
                categoryScores.put(categoryName, categoryScores.getOrDefault(categoryName, 0) + 1);
            }
        }

        if (categoryScores.isEmpty()) {
            return null;
        }

        String bestCategory = categoryScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (bestCategory == null) {
            return null;
        }

        List<InterviewKbCategory> categories = categoryRepository.findAllByOrderByIdAsc();
        for (InterviewKbCategory category : categories) {
            if (bestCategory.equals(category.getName())) {
                return category.getId();
            }
        }

        return null;
    }

    @Transactional
    public void updateCategoryQuestionCounts() {
        List<InterviewKbCategory> categories = categoryRepository.findAllByOrderByIdAsc();
        List<InterviewKbItem> allItems = itemRepository.findAll();

        Map<Long, Integer> counts = new HashMap<>();
        for (InterviewKbItem item : allItems) {
            if (item.getCategoryId() != null) {
                counts.put(item.getCategoryId(), counts.getOrDefault(item.getCategoryId(), 0) + 1);
            }
        }

        for (InterviewKbCategory category : categories) {
            category.setQuestionCount(counts.getOrDefault(category.getId(), 0));
            categoryRepository.save(category);
        }
    }

    private InterviewKbCategoryResponse toCategoryResponse(InterviewKbCategory category) {
        InterviewKbCategoryResponse response = new InterviewKbCategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setParentId(category.getParentId());
        response.setQuestionCount(category.getQuestionCount());
        response.setCreatedAt(category.getCreatedAt());
        return response;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
