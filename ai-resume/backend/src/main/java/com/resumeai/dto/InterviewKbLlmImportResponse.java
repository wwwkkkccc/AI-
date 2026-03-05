package com.resumeai.dto;

/**
 * 面试知识库 LLM 生成导入响应 DTO，用于返回 LLM 生成面试题的执行结果
 */
public class InterviewKbLlmImportResponse {
    /** 面试题主题 */
    private String topic;
    /** 使用的 LLM 模型名称 */
    private String model;
    /** 请求生成的面试题数量 */
    private int requestedCount;
    /** 实际生成的面试题数量 */
    private int generatedCount;
    /** 成功导入知识库的面试题数量 */
    private int importedCount;
    /** 生成的知识库文档信息 */
    private InterviewKbDocItem doc;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public void setRequestedCount(int requestedCount) {
        this.requestedCount = requestedCount;
    }

    public int getGeneratedCount() {
        return generatedCount;
    }

    public void setGeneratedCount(int generatedCount) {
        this.generatedCount = generatedCount;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(int importedCount) {
        this.importedCount = importedCount;
    }

    public InterviewKbDocItem getDoc() {
        return doc;
    }

    public void setDoc(InterviewKbDocItem doc) {
        this.doc = doc;
    }
}
