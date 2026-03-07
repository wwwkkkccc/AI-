package com.resumeai.dto;

import java.util.List;

/**
 * 简历模板列表响应
 */
public class ResumeTemplateResponse {
    private List<ResumeTemplateItem> items;
    private int page;
    private int size;
    private long total;

    public List<ResumeTemplateItem> getItems() {
        return items;
    }

    public void setItems(List<ResumeTemplateItem> items) {
        this.items = items;
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

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
