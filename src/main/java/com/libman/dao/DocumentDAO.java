package com.libman.dao;

import com.libman.model.Document;
import com.libman.model.Magazine;
import java.sql.SQLException;
import com.libman.exception.DocumentNotFoundException;

public interface DocumentDAO {
    void addDocument(Document document) throws SQLException;
    Document getDocumentByTitle(String title) throws SQLException;
    Document getDocumentByAuthor(String author);
    Document getDocumentByGenre(String genre);
    Document getDocumentById(int id);
    void updateDocumentAttributes(Document doc,
                                        String newTitle,
                                        String newAuthor,
                                        String newGenre,
                                        String newIsbn,
                                        Integer newPageNumber,
                                        Integer newNumber,
                                        Magazine.Periodicity newPeriodicity) throws DocumentNotFoundException;
    void removeDocument(Document document);
    void updateDocument(Document document);
    boolean isDocumentBorrowed(int idDoc);
}
