package com.portfolio.erp.partner;

import jakarta.validation.constraints.*;

public class PartnerForm {
    @NotBlank(message = "{validation.code.required}") @Pattern(regexp = "[A-Z0-9_-]{1,40}", message = "{validation.code.pattern}")
    private String code;
    @NotBlank(message = "{validation.name.required}") @Size(max = 150, message = "{validation.name.size}")
    private String name;
    @NotNull(message = "{validation.type}") @Pattern(regexp = "BOOKSTORE|DISTRIBUTOR", message = "{validation.type}")
    private String type;
    @Size(max = 100, message = "{validation.contact.size}")
    private String contactPerson;
    @Size(max = 30, message = "{validation.phone.size}")
    private String phone;
    @Email(message = "{validation.email}") @Size(max = 254, message = "{validation.email.size}")
    private String email;
    @Size(max = 300, message = "{validation.address.size}")
    private String address;
    @NotNull(message = "{validation.active}")
    private Boolean active = true;
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code == null || code.isBlank() ? null : code.trim(); }
    public String getName() { return name; }
    public void setName(String name) { this.name = name == null || name.isBlank() ? null : name.trim(); }
    public String getType() { return type; }
    public void setType(String type) { this.type = type == null || type.isBlank() ? null : type.trim(); }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson == null || contactPerson.isBlank() ? null : contactPerson.trim(); }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone == null || phone.isBlank() ? null : phone.trim(); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null || email.isBlank() ? null : email.trim(); }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address == null || address.isBlank() ? null : address.trim(); }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
