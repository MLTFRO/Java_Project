package com.libman.model;

import java.time.LocalDate;

public class Borrow {

    private String id;
    private Document document;
    private Member member;
    private LocalDate borrowDate;
    private LocalDate expectedReturnDate;
    private LocalDate returnDate;

    public Borrow(Document document, Member member, LocalDate borrowDate) {
        this.document = document;
        this.member = member;
        this.borrowDate = borrowDate;
        this.expectedReturnDate = borrowDate.plusDays(14); 
        this.returnDate = null; 
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Document getDocument() {
        return document;
    }

    public Member getMember() {
        return member;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public String getBorrowDateString() {
        return borrowDate.toString();
    }

    public LocalDate getExpectedReturnDate() {
        return expectedReturnDate;
    }

    public String getExpectedReturnDateString() {
        return expectedReturnDate.toString();
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public String getReturnDateString() {
        return returnDate.toString();
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public void setExpectedReturnDate(LocalDate expectedReturnDate) {
        this.expectedReturnDate = expectedReturnDate;
    }

    public boolean isReturned() {
        return returnDate != null;
    }

    public boolean isOverdue() {
        LocalDate checkDate = (returnDate != null) ? returnDate : LocalDate.now();
        return checkDate.isAfter(expectedReturnDate);
    }

    @Override
    public String toString() {
        return "Borrow{" +
                "book=" + (document != null ? document.getTitle() : "N/A") +
                ", member=" + (member != null ? member.getName() : "N/A") +
                ", borrowDate=" + borrowDate +
                ", expectedReturn=" + expectedReturnDate +
                ", returnDate=" + returnDate +
                '}';
    }
}