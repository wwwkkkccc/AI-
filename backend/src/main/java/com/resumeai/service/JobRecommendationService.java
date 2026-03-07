package com.resumeai.service;

import com.resumeai.dto.JobRecommendationResponse;
import com.resumeai.model.UserAccount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 岗位推荐服务
 */
@Service
public class JobRecommendationService {

    // 预定义岗位模板
    private static final Map<String, List<String>> JOB_TEMPLATES = new HashMap<>();

    static {
        JOB_TEMPLATES.put("Java后端工程师", Arrays.asList(
                "java", "spring", "springboot", "mysql", "redis", "mybatis", "maven",
                "微服务", "分布式", "restful", "api", "后端", "服务端"));

        JOB_TEMPLATES.put("前端工程师", Arrays.asList(
                "javascript", "vue", "react", "angular", "html", "css", "typescript",
                "webpack", "前端", "ui", "页面", "组件", "node"));

        JOB_TEMPLATES.put("全栈工程师", Arrays.asList(
                "java", "javascript", "vue", "react", "spring", "mysql", "前端", "后端",
                "全栈", "node", "express", "api", "restful"));

        JOB_TEMPLATES.put("数据工程师", Arrays.asList(
                "python", "spark", "hadoop", "hive", "sql", "etl", "数据仓库",
                "数据分析", "大数据", "kafka", "flink", "数据处理"));

        JOB_TEMPLATES.put("DevOps工程师", Arrays.asList(
                "docker", "kubernetes", "jenkins", "ci/cd", "linux", "shell", "运维",
                "自动化", "监控", "部署", "容器", "k8s", "devops"));

        JOB_TEMPLATES.put("产品经理", Arrays.asList(
                "需求分析", "原型设计", "产品规划", "用户研究", "axure", "产品",
                "项目管理", "敏捷", "scrum", "prd", "用户体验", "交互"));

        JOB_TEMPLATES.put("测试工程师", Arrays.asList(
                "测试", "自动化测试", "selenium", "junit", "testng", "性能测试",
                "接口测试", "jmeter", "质量保证", "qa", "bug", "用例"));

        JOB_TEMPLATES.put("算法工程师", Arrays.asList(
                "机器学习", "深度学习", "tensorflow", "pytorch", "python", "算法",
                "神经网络", "nlp", "cv", "数据挖掘", "模型", "ai"));

        JOB_TEMPLATES.put("架构师", Arrays.asList(
                "系统设计", "架构设计", "微服务", "分布式", "高并发", "高可用",
                "技术选型", "性能优化", "架构", "中间件", "缓存", "消息队列"));

        JOB_TEMPLATES.put("项目经理", Arrays.asList(
                "项目管理", "团队管理", "敏捷", "scrum", "pmp", "风险管理",
                "进度管理", "沟通协调", "资源管理", "项目", "计划", "交付"));
    }

    private final LlmClient llmClient;

    public JobRecommendationService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * 推荐岗位
     */
    public List<JobRecommendationResponse> recommendJobs(String resumeText, Long userId) {
        if (resumeText == null || resumeText.trim().isEmpty()) {
            throw new IllegalArgumentException("resume text is required");
        }

        // 提取简历中的技能关键词
        Set<String> resumeSkills = extractSkills(resumeText);

        // 计算每个岗位的匹配度
        List<JobRecommendationResponse> recommendations = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : JOB_TEMPLATES.entrySet()) {
            String roleName = entry.getKey();
            List<String> requiredSkills = entry.getValue();

            // 计算匹配的技能
            List<String> matchedSkills = new ArrayList<>();
            List<String> missingSkills = new ArrayList<>();

            for (String skill : requiredSkills) {
                if (resumeSkills.contains(skill.toLowerCase(Locale.ROOT))) {
                    matchedSkills.add(skill);
                } else {
                    missingSkills.add(skill);
                }
            }

            // 计算匹配分数
            double matchScore = requiredSkills.isEmpty() ? 0.0 :
                    (double) matchedSkills.size() / requiredSkills.size() * 100;

            // 只保留匹配度 > 10% 的岗位
            if (matchScore > 10) {
                JobRecommendationResponse recommendation = new JobRecommendationResponse();
                recommendation.setRoleName(roleName);
                recommendation.setMatchScore(Math.round(matchScore * 10) / 10.0);
                recommendation.setMatchedSkills(matchedSkills);
                recommendation.setMissingSkills(missingSkills);
                recommendation.setReason(generateLocalReason(roleName, matchedSkills, missingSkills));
                recommendations.add(recommendation);
            }
        }

        // 按匹配度降序排序，取前5个
        recommendations.sort(Comparator.comparing(
                JobRecommendationResponse::getMatchScore, Comparator.reverseOrder()));

        List<JobRecommendationResponse> topRecommendations = recommendations.stream()
                .limit(5)
                .collect(Collectors.toList());

        // 尝试使用 LLM 增强推荐理由
        enhanceWithLlm(topRecommendations, resumeText);

        return topRecommendations;
    }

    /**
     * 从简历文本中提取技能关键词
     */
    private Set<String> extractSkills(String text) {
        Set<String> skills = new HashSet<>();
        String lowerText = text.toLowerCase(Locale.ROOT);

        // 收集所有预定义的技能关键词
        Set<String> allSkills = new HashSet<>();
        for (List<String> jobSkills : JOB_TEMPLATES.values()) {
            allSkills.addAll(jobSkills);
        }

        // 检查简历中是否包含这些技能
        for (String skill : allSkills) {
            if (lowerText.contains(skill.toLowerCase(Locale.ROOT))) {
                skills.add(skill.toLowerCase(Locale.ROOT));
            }
        }

        return skills;
    }

    /**
     * 生成本地推荐理由
     */
    private String generateLocalReason(String roleName, List<String> matched, List<String> missing) {
        StringBuilder reason = new StringBuilder();
        reason.append("您具备 ").append(matched.size()).append(" 项相关技能");

        if (!missing.isEmpty() && missing.size() <= 3) {
            reason.append("，建议补充：").append(String.join("、", missing.subList(0, Math.min(3, missing.size()))));
        }

        return reason.toString();
    }

    /**
     * 使用 LLM 增强推荐理由
     */
    private void enhanceWithLlm(List<JobRecommendationResponse> recommendations, String resumeText) {
        if (recommendations.isEmpty()) {
            return;
        }

        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("根据以下简历内容，为候选人提供岗位推荐的个性化建议。\n\n");
            prompt.append("简历摘要：\n").append(truncate(resumeText, 1000)).append("\n\n");
            prompt.append("推荐岗位：\n");

            for (int i = 0; i < recommendations.size(); i++) {
                JobRecommendationResponse rec = recommendations.get(i);
                prompt.append(i + 1).append(". ").append(rec.getRoleName())
                        .append("（匹配度：").append(rec.getMatchScore()).append("%）\n");
            }

            prompt.append("\n请为每个岗位生成一句话的个性化推荐理由（30字以内），格式：\n");
            prompt.append("1. [推荐理由]\n2. [推荐理由]\n...");

            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt.toString());

            String llmResponse = llmClient.chat(List.of(message), 0.7, 15);

            // 解析 LLM 响应并更新推荐理由
            if (llmResponse != null && !llmResponse.trim().isEmpty()) {
                String[] lines = llmResponse.split("\n");
                int index = 0;
                for (String line : lines) {
                    line = line.trim();
                    if (line.matches("^\\d+\\.\\s+.+")) {
                        String reason = line.replaceFirst("^\\d+\\.\\s+", "").trim();
                        if (index < recommendations.size() && !reason.isEmpty()) {
                            recommendations.get(index).setReason(reason);
                            index++;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // LLM 调用失败，保持本地生成的理由
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
