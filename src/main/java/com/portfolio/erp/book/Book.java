package com.portfolio.erp.book;

import java.time.LocalDateTime;

public class Book extends BookForm {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public static Book from(BookForm form) {
        var entity = new Book();
        entity.setIsbn(form.getIsbn());
        entity.setTitle(form.getTitle());
        entity.setAuthor(form.getAuthor());
        entity.setPublisher(form.getPublisher());
        entity.setPublicationDate(form.getPublicationDate());
        entity.setListPrice(form.getListPrice());
        entity.setActive(form.getActive());
        return entity;
    }
}
