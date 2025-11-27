package com.libman.model;

public class Book extends Document {

    private String isbn;
    private int pageNumber;
    private Document document;

    public Book(Document document, String isbn, int pageNumber) {
        super(document.getTitle(), document.getAuthor(), document.getGenre());
        this.document = document;
        this.isbn = isbn;
        this.pageNumber = pageNumber;
    }
    
    public Book(String title, String author, String genre, String isbn, int pageNumber) {
        super(title, author, genre); // Call Document constructor
        this.isbn = isbn;
        this.pageNumber = pageNumber;
    }

    public Book(){
        super(null,null,null);
        this.isbn = null;
        this.pageNumber = 0;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    @Override
    public String toString() {
        return "Book{" +
                "title='" + getTitle() + '\'' +
                ", author='" + getAuthor() + '\'' +
                ", genre='" + getGenre() + '\'' +
                ", isbn='" + isbn + '\'' +
                ", pages=" + pageNumber +
                '}';
    }
}
