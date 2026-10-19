package com.library.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "copies")
public class BookCopy {

    @Id
    @Column(nullable = false, unique = true)
    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_isbn", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CopyStatus status = CopyStatus.AVAILABLE;

    public BookCopy() {
    }

    public BookCopy(String barcode, Book book, CopyStatus status) {
        this.barcode = barcode;
        this.book = book;
        this.status = status != null ? status : CopyStatus.AVAILABLE;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public CopyStatus getStatus() {
        return status;
    }

    public void setStatus(CopyStatus status) {
        this.status = status;
    }
}
