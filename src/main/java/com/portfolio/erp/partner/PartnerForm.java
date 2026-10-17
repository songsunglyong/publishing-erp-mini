package com.portfolio.erp.partner;

import jakarta.validation.constraints.*;

public class PartnerForm {
    @NotBlank(message = "거래처 코드를 입력하세요.") @Pattern(regexp = "[A-Z0-9_-]{1,40}", message = "코드는 영문 대문자·숫자·밑줄·하이픈 40자 이내로 입력하세요.")
    private String code;
    @NotBlank(message = "거래처명을 입력하세요.") @Size(max = 150, message = "거래처명은 150자 이하여야 합니다.")
    private String name;
    @NotNull(message = "거래처 유형을 선택하세요.") @Pattern(regexp = "BOOKSTORE|DISTRIBUTOR", message = "올바른 거래처 유형을 선택하세요.")
    private String type;
    @Size(max = 100, message = "담당자는 100자 이하여야 합니다.")
    private String contactPerson;
    @Size(max = 30, message = "전화번호는 30자 이하여야 합니다.")
    private String phone;
    @Email(message = "올바른 이메일을 입력하세요.") @Size(max = 254, message = "이메일은 254자 이하여야 합니다.")
    private String email;
    @Size(max = 300, message = "주소는 300자 이하여야 합니다.")
    private String address;
    @NotNull(message = "사용 상태를 선택하세요.")
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
