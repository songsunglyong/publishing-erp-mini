package com.portfolio.erp.partner;

import java.time.LocalDateTime;

public class Partner extends PartnerForm {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public static Partner from(PartnerForm form) {
        var entity = new Partner();
        entity.setCode(form.getCode());
        entity.setName(form.getName());
        entity.setType(form.getType());
        entity.setContactPerson(form.getContactPerson());
        entity.setPhone(form.getPhone());
        entity.setEmail(form.getEmail());
        entity.setAddress(form.getAddress());
        entity.setActive(form.getActive());
        return entity;
    }
}
