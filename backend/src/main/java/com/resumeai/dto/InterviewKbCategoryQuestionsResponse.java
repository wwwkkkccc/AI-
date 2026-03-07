package com.resumeai.dto;

import java.util.List;

/**
 * 分类题目列表响应
 */
public class InterviewKbCategoryQuestionsResponse {
    private List<InterviewKbQuestionItem> items;
    private long total;
    private int page;
    private int size;

    public List<InterviewKbQuestionItem> getItems() {
        return items;
    }

    public void setItems(List<InterviewKbQuestionItem> items) {
        this.items = items;
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
}
