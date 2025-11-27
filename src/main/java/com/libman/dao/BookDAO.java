package com.libman.dao;

import com.libman.model.Book;
import com.libman.exception.DocumentNotFoundException;
import java.util.List;

public interface BookDAO {
    void addBook(Book book) throws DocumentNotFoundException;
    Book getBookByIsbn(String isbn) throws DocumentNotFoundException;
    List<Book> getAllBooks();
    void updateBookAttributes(Book book, String newTitle, String newAuthor, String newGenre,
                              String newIsbn, Integer newPageNumber) throws DocumentNotFoundException;
    void removeBook(Book book) throws DocumentNotFoundException;
}