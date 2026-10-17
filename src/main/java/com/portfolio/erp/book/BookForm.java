package com.portfolio.erp.book;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.portfolio.erp.common.Isbn13;

public class BookForm {
    @Isbn13
    private String isbn;
    @NotBlank(message = "도서명을 입력하세요.") @Size(max = 200, message = "도서명은 200자 이하여야 합니다.")
    private String title;
    @Size(max = 100, message = "저자는 100자 이하여야 합니다.")
    private String author;
    @Size(max = 100, message = "출판사는 100자 이하여야 합니다.")
    private String publisher;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate publicationDate;
    @NotNull(message = "정가를 입력하세요.") @Min(value = 0, message = "정가는 0 이상이어야 합니다.") @Max(value = 999999999999L, message = "정가가 허용 범위를 초과했습니다.")
    private Long listPrice;
    @NotNull(message = "사용 상태를 선택하세요.")
    private Boolean active = true;
    @AssertTrue(message = "발행일은 1000-01-01부터 9999-12-31 사이여야 합니다.")
    public boolean isPublicationDateSupported() {
        return publicationDate == null || (publicationDate.getYear() >= 1000 && publicationDate.getYear() <= 9999);
    }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn == null || isbn.isBlank() ? null : isbn.trim(); }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title == null || title.isBlank() ? null : title.trim(); }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author == null || author.isBlank() ? null : author.trim(); }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher == null || publisher.isBlank() ? null : publisher.trim(); }
    public LocalDate getPublicationDate() { return publicationDate; }
    public void setPublicationDate(LocalDate publicationDate) { this.publicationDate = publicationDate; }
    public Long getListPrice() { return listPrice; }
    public void setListPrice(Long listPrice) { this.listPrice = listPrice; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
