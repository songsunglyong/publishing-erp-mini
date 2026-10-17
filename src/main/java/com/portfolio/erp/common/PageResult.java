package com.portfolio.erp.common;

import java.util.List;

public class PageResult<T> {
    private final List<T> items;
    private final long total;
    private final int page;
    private final int pageSize;
    public PageResult(List<T> items, long total, PageQuery query) {
        this.items = List.copyOf(items); this.total = total;
        this.page = query.getPage(); this.pageSize = query.getLimit();
    }
    public List<T> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public long getTotalPages() { return Math.max(1, (total + pageSize - 1) / pageSize); }
    public boolean getHasPrevious() { return page > 1; }
    public boolean getHasNext() { return page < getTotalPages(); }
}
