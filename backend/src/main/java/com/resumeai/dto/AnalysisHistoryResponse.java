package com.resumeai.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 简历分析历史列表响应 DTO，用于分页返回分析历史记录
 */
public class AnalysisHistoryResponse {
    /** 当前页的分析历史列表 */
    private List<AnalysisHistoryItem> items = new ArrayList<>();
    /** 记录总数 */
    private long total;
    /** 当前页码 */
    private int page;
    /** 每页大小 */
    private int size;

    public List<AnalysisHistoryItem> getItems() {
        return items;
    }

    public void setItems(List<AnalysisHistoryItem> items) {
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
