package com.libman.dao;

import com.libman.model.Document;
import com.libman.model.Magazine.Periodicity;
import com.libman.model.Book;
import com.libman.exception.DocumentNotFoundException;

import java.sql.SQLException;

public interface DocumentDAO {

    // -------------------- CREATE --------------------
    void addDocument(Document document) throws SQLException;

    // -------------------- READ --------------------
    Document getDocumentById(int id);
    Document getDocumentByTitle(String title) throws SQLException;
    Document getDocumentByAuthor(String author);
    Document getDocumentByGenre(String genre);
    Book getBookByIsbn(String isbn);

    // -------------------- UPDATE --------------------
    void updateDocument(Document document);
    void updateDocumentAttributes(Document doc,
                                  String newTitle,
                                  String newAuthor,
                                  String newGenre,
                                  String newIsbn,
                                  Integer newPageNumber,
                                  Integer newNumber,
                                  Periodicity newPeriodicity) throws DocumentNotFoundException;

    // -------------------- DELETE --------------------
    void removeDocument(Document document);

    // -------------------- UTILITY --------------------
    boolean isDocumentBorrowed(int idDoc);
}