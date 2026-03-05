package com.resumeai.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 面试知识库网页爬取响应 DTO，用于返回爬取任务的执行结果
 */
public class InterviewKbCrawlResponse {
    /** 爬取的起始 URL */
    private String seedUrl;
    /** 已访问的页面数 */
    private int pagesVisited;
    /** 成功抓取的页面数 */
    private int pagesSucceeded;
    /** 提取到的面试题数量 */
    private int questionsExtracted;
    /** 成功导入知识库的面试题数量 */
    private int questionsImported;
    /** 生成的知识库文档信息 */
    private InterviewKbDocItem doc;
    /** 爬取过程中的错误信息列表 */
    private List<String> errors = new ArrayList<>();

    public String getSeedUrl() {
        return seedUrl;
    }

    public void setSeedUrl(String seedUrl) {
        this.seedUrl = seedUrl;
    }

    public int getPagesVisited() {
        return pagesVisited;
    }

    public void setPagesVisited(int pagesVisited) {
        this.pagesVisited = pagesVisited;
    }

    public int getPagesSucceeded() {
        return pagesSucceeded;
    }

    public void setPagesSucceeded(int pagesSucceeded) {
        this.pagesSucceeded = pagesSucceeded;
    }

    public int getQuestionsExtracted() {
        return questionsExtracted;
    }

    public void setQuestionsExtracted(int questionsExtracted) {
        this.questionsExtracted = questionsExtracted;
    }

    public int getQuestionsImported() {
        return questionsImported;
    }

    public void setQuestionsImported(int questionsImported) {
        this.questionsImported = questionsImported;
    }

    public InterviewKbDocItem getDoc() {
        return doc;
    }

    public void setDoc(InterviewKbDocItem doc) {
        this.doc = doc;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
