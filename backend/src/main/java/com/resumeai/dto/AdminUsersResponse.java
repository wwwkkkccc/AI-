package com.resumeai.dto;

import java.util.ArrayList;
import java.util.List;

public class AdminUsersResponse {
    private List<AdminUserItem> items = new ArrayList<>();
    private long total;
    private int page;
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
