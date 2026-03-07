package com.resumeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.resumeai.dto.AnalyzeResponse;
import com.resumeai.dto.OptimizedBlock;
import com.resumeai.model.AnalysisRecord;
import com.resumeai.repository.AnalysisRecordRepository;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AnalysisRecordRepository analysisRecordRepository;
    private final ObjectMapper objectMapper;

    public ExportService(AnalysisRecordRepository analysisRecordRepository, ObjectMapper objectMapper) {
        this.analysisRecordRepository = analysisRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public String exportMarkdown(Long analysisId, Long userId, boolean adminMode) {
        AnalysisRecord record = analysisRecordRepository.findById(analysisId)
                .orElseThrow(() -> new IllegalArgumentException("analysis not found"));

        if (!adminMode && !record.getUserId().equals(userId)) {
            throw new ForbiddenException("cannot access this analysis");
        }

        AnalyzeResponse result = null;
        if (record.getResultJson() != null && !record.getResultJson().isBlank()) {
            try {
                result = objectMapper.readValue(record.getResultJson(), AnalyzeResponse.class);
            } catch (Exception ignore) {
                // 解析失败则使用基础信息
            }
        }

        StringBuilder md = new StringBuilder();
        md.append("# 简历分析报告\n\n");

        md.append("## 基本信息\n\n");
        md.append("- **分析时间**: ").append(formatDate(record.getCreatedAt())).append("\n");
        md.append("- **文件名**: ").append(clean(record.getFilename())).append("\n");
        if (record.getTargetRole() != null && !record.getTargetRole().isBlank()) {
            md.append("- **目标岗位**: ").append(clean(record.getTargetRole())).append("\n");
        }
        md.append("- **使用AI模型**: ").append(Boolean.TRUE.equals(record.getModelUsed()) ? "是" : "否").append("\n");
        md.append("\n");

        md.append("## 评分结果\n\n");
        md.append("- **综合评分**: ").append(String.format("%.1f", record.getScore())).append(" / 100\n");
        md.append("- **关键词覆盖率**: ").append(String.format("%.1f%%", record.getCoverage())).append("\n");
        md.append("\n");

        if (result != null) {
            if (result.getMatchedKeywords() != null && !result.getMatchedKeywords().isEmpty()) {
                md.append("## 已匹配关键词\n\n");
                for (String kw : result.getMatchedKeywords()) {
                    md.append("- ").append(clean(kw)).append("\n");
                }
                md.append("\n");
            }

            if (result.getMissingKeywords() != null && !result.getMissingKeywords().isEmpty()) {
                md.append("## 缺失关键词\n\n");
                for (String kw : result.getMissingKeywords()) {
                    md.append("- ").append(clean(kw)).append("\n");
                }
                md.append("\n");
            }

            OptimizedBlock optimized = result.getOptimized();
            if (optimized != null) {
                if (optimized.getSummary() != null && !optimized.getSummary().isBlank()) {
                    md.append("## 优化建议摘要\n\n");
                    md.append(clean(optimized.getSummary())).append("\n\n");
                }

                if (optimized.getRewrittenExperience() != null && !optimized.getRewrittenExperience().isEmpty()) {
                    md.append("## 工作经历优化\n\n");
                    for (String exp : optimized.getRewrittenExperience()) {
                        md.append("- ").append(clean(exp)).append("\n");
                    }
                    md.append("\n");
                }

                if (optimized.getSkillsRecommendations() != null && !optimized.getSkillsRecommendations().isEmpty()) {
                    md.append("## 技能建议\n\n");
                    for (String skill : optimized.getSkillsRecommendations()) {
                        md.append("- ").append(clean(skill)).append("\n");
                    }
                    md.append("\n");
                }

                if (optimized.getInterviewQuestions() != null && !optimized.getInterviewQuestions().isEmpty()) {
                    md.append("## 面试题准备\n\n");
                    int qNum = 1;
                    for (String q : optimized.getInterviewQuestions()) {
                        md.append(qNum++).append(". ").append(clean(q)).append("\n");
                    }
                    md.append("\n");
                }
            }
        }

        md.append("---\n\n");
        md.append("*本报告由 Resume AI 系统生成*\n");

        return md.toString();
    }

    @Transactional(readOnly = true)
    public byte[] exportPdf(Long analysisId, Long userId, boolean adminMode) {
        String markdown = exportMarkdown(analysisId, userId, adminMode);
        String html = markdownToHtml(markdown);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception ex) {
            throw new IllegalArgumentException("failed to generate pdf: " + ex.getMessage());
        }
    }

    private String markdownToHtml(String markdown) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\"/>\n");
        html.append("<style>\n");
        html.append("body { font-family: 'SimSun', serif; margin: 40px; line-height: 1.6; }\n");
        html.append("h1 { color: #333; border-bottom: 2px solid #333; padding-bottom: 10px; }\n");
        html.append("h2 { color: #555; margin-top: 30px; border-bottom: 1px solid #ddd; padding-bottom: 5px; }\n");
        html.append("ul, ol { margin-left: 20px; }\n");
        html.append("li { margin-bottom: 5px; }\n");
        html.append("hr { border: none; border-top: 1px solid #ccc; margin: 30px 0; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");

        // 简单的 Markdown 转 HTML（仅支持基本语法）
        String[] lines = markdown.split("\n");
        boolean inList = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("# ")) {
                if (inList) { html.append("</ul>\n"); inList = false; }
                html.append("<h1>").append(escapeHtml(trimmed.substring(2))).append("</h1>\n");
            } else if (trimmed.startsWith("## ")) {
                if (inList) { html.append("</ul>\n"); inList = false; }
                html.append("<h2>").append(escapeHtml(trimmed.substring(3))).append("</h2>\n");
            } else if (trimmed.startsWith("- ")) {
                if (!inList) { html.append("<ul>\n"); inList = true; }
                html.append("<li>").append(escapeHtml(trimmed.substring(2))).append("</li>\n");
            } else if (trimmed.matches("^\\d+\\.\\s.*")) {
                if (inList) { html.append("</ul>\n"); inList = false; }
                String content = trimmed.replaceFirst("^\\d+\\.\\s", "");
                html.append("<p>").append(escapeHtml(content)).append("</p>\n");
            } else if (trimmed.equals("---")) {
                if (inList) { html.append("</ul>\n"); inList = false; }
                html.append("<hr/>\n");
            } else if (!trimmed.isEmpty()) {
                if (inList && !trimmed.startsWith("- ")) {
                    html.append("</ul>\n");
                    inList = false;
                }
                html.append("<p>").append(escapeHtml(trimmed)).append("</p>\n");
            } else {
                if (inList) { html.append("</ul>\n"); inList = false; }
            }
        }

        if (inList) {
            html.append("</ul>\n");
        }

        html.append("</body>\n</html>");
        return html.toString();
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("*", "");
    }

    private String formatDate(java.time.Instant instant) {
        if (instant == null) return "";
        return instant.atZone(ZoneId.systemDefault()).format(DATE_FORMATTER);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
