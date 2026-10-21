package com.portfolio.erp.book;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.portfolio.erp.common.Isbn13;

public class BookForm {
    @Isbn13
    private String isbn;
    @NotBlank(message = "{validation.title.required}") @Size(max = 200, message = "{validation.title.size}")
    private String title;
    @Size(max = 100, message = "{validation.author.size}")
    private String author;
    @Size(max = 100, message = "{validation.publisher.size}")
    private String publisher;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate publicationDate;
    @NotNull(message = "{validation.price.required}") @Min(value = 0, message = "{validation.price.min}") @Max(value = 999999999999L, message = "{validation.price.max}")
    private Long listPrice;
    @NotNull(message = "{validation.active}")
    private Boolean active = true;
    @AssertTrue(message = "{validation.date.range}")
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
