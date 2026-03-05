package com.resumeai.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 面试知识库文档面试题列表响应 DTO，用于分页返回指定文档中的面试题
 */
public class InterviewKbDocQuestionsResponse {
    /** 文档 ID */
    private Long docId;
    /** 文档标题 */
    private String title;
    /** 文件名 */
    private String filename;
    /** 面试题总数 */
    private long total;
    /** 当前页码 */
    private int page;
    /** 每页大小 */
    private int size;
    /** 当前页的面试题列表 */
    private List<String> questions = new ArrayList<>();

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<String> getQuestions() {
        return questions;
    }

    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }
}
