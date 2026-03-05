package com.resumeai.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 面试知识库文档列表响应 DTO，用于分页返回知识库文档列表
 */
public class InterviewKbDocsResponse {
    /** 当前页的文档列表 */
    private List<InterviewKbDocItem> items = new ArrayList<>();
    /** 文档总数 */
    private long total;
    /** 当前页码 */
    private int page;
    /** 每页大小 */
    private int size;

    public List<InterviewKbDocItem> getItems() {
        return items;
    }

    public void setItems(List<InterviewKbDocItem> items) {
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
