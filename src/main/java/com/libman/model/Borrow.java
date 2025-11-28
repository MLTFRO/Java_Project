package com.libman.model;
import java.time.LocalDate;

public class Borrow {
    private String id;
    private int idDoc;      // Foreign key to Document
    private int idMember;   // Foreign key to Member
    private Document document;
    private Member member;
    private LocalDate borrowDate;
    private LocalDate expectedReturnDate;
    private LocalDate returnDate;

    // Default constructor for DAO usage
    public Borrow() {
    }

    public Borrow(Document document, Member member, LocalDate borrowDate) {
        this.document = document;
        this.member = member;
        if (document != null) {
            this.idDoc = document.getIdDoc();
        }
        if (member != null) {
            this.idMember = member.getIdMember();
        }
        this.borrowDate = borrowDate;
        this.expectedReturnDate = borrowDate.plusDays(14); 
        this.returnDate = null; 
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getIdDoc() { return idDoc; }
    public void setIdDoc(int idDoc) {
        this.idDoc = idDoc;
        // Also update the nested document object if it exists
        if (this.document != null) {
            this.document.setIdDoc(idDoc);
        }
    }

    public void setIdMember(int idMember) {
        this.idMember = idMember;
        // Also update the nested member object if it exists
        if (this.member != null) {
            this.member.setIdMember(idMember);
        }
    }

    public int getIdMember() { return idMember; }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
        if (document != null) {
            this.idDoc = document.getIdDoc();
        }
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
        if (member != null) {
            this.idMember = member.getIdMember();
        }
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDate borrowDate) {
        this.borrowDate = borrowDate;
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
        return returnDate != null ? returnDate.toString() : "Not returned";
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
                "id='" + id + '\'' +
                ", idDoc=" + idDoc +
                ", idMember=" + idMember +
                ", book=" + (document != null ? document.getTitle() : "N/A") +
                ", member=" + (member != null ? member.getName() : "N/A") +
                ", borrowDate=" + borrowDate +
                ", expectedReturn=" + expectedReturnDate +
                ", returnDate=" + returnDate +
                '}';
    }
}