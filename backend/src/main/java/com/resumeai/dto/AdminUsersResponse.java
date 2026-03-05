package com.resumeai.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员用户列表响应 DTO，用于分页返回用户列表数据
 */
public class AdminUsersResponse {
    /** 当前页的用户列表 */
    private List<AdminUserItem> items = new ArrayList<>();
    /** 用户总数 */
    private long total;
    /** 当前页码 */
    private int page;
    /** 每页大小 */
    private int size;

    public List<AdminUserItem> getItems() {
        return items;
    }

    public void setItems(List<AdminUserItem> items) {
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
