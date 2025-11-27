package com.libman.dao;

import java.sql.*;

import com.libman.model.Document;
import com.libman.model.Book;
import com.libman.model.Magazine;
import com.libman.model.Magazine.Periodicity;
import com.libman.exception.DocumentNotFoundException;

public class DocumentDAOImpl implements DocumentDAO {

    private Connection conn;

    public DocumentDAOImpl() {
        this.conn = DatabaseManager.getConnection();
    }

    // -------------------- ADD --------------------
    public void addDocument(Document document) throws SQLException {
        String sqlDoc = "INSERT INTO Document (title, author, genre) VALUES (?, ?, ?)";
        try (PreparedStatement stmtDoc = conn.prepareStatement(sqlDoc, Statement.RETURN_GENERATED_KEYS)) {
            stmtDoc.setString(1, document.getTitle());
            stmtDoc.setString(2, document.getAuthor());
            stmtDoc.setString(3, document.getGenre());
            stmtDoc.executeUpdate();

            // Retrieve generated id_doc
            try (ResultSet keys = stmtDoc.getGeneratedKeys()) {
                if (keys.next()) {
                    int idDoc = keys.getInt(1);

                    // Set the ID in the parent Document object
                    document.setIdDoc(idDoc);

                    // Also ensure Book or Magazine has the same ID
                    if (document instanceof Book book) {
                        book.setIdDoc(idDoc); // important for borrowed checks

                        String sqlBook = "INSERT INTO Book (id_doc, title, isbn, pageNumber, author, genre) VALUES (?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement stmtBook = conn.prepareStatement(sqlBook)) {
                            stmtBook.setInt(1, idDoc);
                            stmtBook.setString(2, book.getTitle());
                            stmtBook.setString(3, book.getIsbn());
                            stmtBook.setInt(4, book.getPageNumber());
                            stmtBook.setString(5, book.getAuthor());
                            stmtBook.setString(6, book.getGenre());
                            stmtBook.executeUpdate();
                        }

                    } else if (document instanceof Magazine mag) {
                        mag.setIdDoc(idDoc); // also set ID for Magazine

                        String sqlMag = "INSERT INTO Magazine (id_doc, title, author, genre, number, periodicity) VALUES (?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement stmtMag = conn.prepareStatement(sqlMag)) {
                            stmtMag.setInt(1, idDoc);
                            stmtMag.setString(2, mag.getTitle());
                            stmtMag.setString(3, mag.getAuthor());
                            stmtMag.setString(4, mag.getGenre());
                            stmtMag.setInt(5, mag.getNumber());
                            stmtMag.setString(6, mag.getPeriodicity().name());
                            stmtMag.executeUpdate();
                        }
                    }
                } else {
                    throw new SQLException("Failed to retrieve generated id_doc.");
                }
            }
        }
    }

    // -------------------- GET --------------------
    @Override
    public Document getDocumentByTitle(String title) throws SQLException {
        String sql = "SELECT * FROM Document WHERE title = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Document doc;
                if ("Book".equals(rs.getString("type"))) {
                    doc = new Book();
                    ((Book) doc).setIsbn(rs.getString("isbn"));
                    ((Book) doc).setPageNumber(rs.getInt("pageNumber"));
                } else {
                    doc = new Magazine();
                    ((Magazine) doc).setNumber(rs.getInt("number"));
                    String periodicityStr = rs.getString("periodicity");
                    if (periodicityStr != null) {
                        ((Magazine) doc).setPeriodicity(Periodicity.valueOf(periodicityStr));
                    }
                }
                doc.setIdDoc(rs.getInt("id_doc"));  // crucial
                doc.setTitle(rs.getString("title"));
                doc.setAuthor(rs.getString("author"));
                doc.setGenre(rs.getString("genre"));
                doc.setAvailability(rs.getBoolean("availability"));
                return doc;
            }
        }
        return null;
    }

    public Document getDocumentByAuthor(String author) {
        try {
            String sql = "SELECT id_doc FROM Document WHERE author = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, author);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int idDoc = rs.getInt("id_doc");
                return getFullDocumentById(idDoc);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Document getDocumentById(int id) {
        if (id <= 0) return null;

        try {
            // 1️⃣ Get base document
            String sqlDoc = "SELECT * FROM Document WHERE id_doc = ?";
            Document baseDoc = null;
            try (PreparedStatement stmt = conn.prepareStatement(sqlDoc)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) return null;

                String title = rs.getString("title");
                String author = rs.getString("author");
                String genre = rs.getString("genre");

                baseDoc = new Document(title, author, genre) {};
                baseDoc.setIdDoc(id);
            }

            // 2️⃣ Determine availability
            String sqlAvail = "SELECT COUNT(*) FROM Borrow WHERE id_doc = ? AND returnDate IS NULL";
            try (PreparedStatement stmt = conn.prepareStatement(sqlAvail)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                boolean available = rs.next() && rs.getInt(1) == 0;
                baseDoc.setAvailability(available);
            }

            // 3️⃣ Check if it's a Book
            String sqlBook = "SELECT * FROM Book WHERE id_doc = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlBook)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String isbn = rs.getString("isbn");
                    int pageNumber = rs.getInt("pageNumber");
                    return new Book(baseDoc, isbn, pageNumber);
                }
            }

            // 4️⃣ Check if it's a Magazine
            String sqlMag = "SELECT * FROM Magazine WHERE id_doc = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlMag)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int number = rs.getInt("number");
                    String perStr = rs.getString("periodicity");
                    Magazine.Periodicity periodicity = Magazine.Periodicity.valueOf(perStr.toUpperCase());
                    Magazine mag = new Magazine(baseDoc.getTitle(), baseDoc.getAuthor(), baseDoc.getGenre(), number, periodicity);
                    mag.setIdDoc(id);
                    mag.setAvailability(baseDoc.isAvailable());
                    return mag;
                }
            }

            // 5️⃣ Return base document if not Book or Magazine
            return baseDoc;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Document getDocumentByGenre(String genre) {
        try {
            String sql = "SELECT id_doc FROM Document WHERE genre = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, genre);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int idDoc = rs.getInt("id_doc");
                return getFullDocumentById(idDoc);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Book getBookByIsbn(String isbn) {
        try {
            String sql = "SELECT id_doc FROM Book WHERE isbn = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, isbn);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int idDoc = rs.getInt("id_doc");
                Document doc = getFullDocumentById(idDoc);
                if (doc instanceof Book) return (Book) doc;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new DocumentNotFoundException("No book found with ISBN = " + isbn);
    }

    // -------------------- UPDATE --------------------
    public void updateDocumentAttributes(Document doc,
                                         String newTitle,
                                         String newAuthor,
                                         String newGenre,
                                         String newIsbn,
                                         Integer newPageNumber,
                                         Integer newNumber,
                                         Periodicity newPeriodicity) {
        if (doc == null) throw new DocumentNotFoundException("Cannot update: document is null");

        try {
            int idDoc = getIdDocForDocument(doc);

            String sql = "UPDATE Document SET title = ?, author = ?, genre = ? WHERE id_doc = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newTitle != null ? newTitle : doc.getTitle());
            stmt.setString(2, newAuthor != null ? newAuthor : doc.getAuthor());
            stmt.setString(3, newGenre != null ? newGenre : doc.getGenre());
            stmt.setInt(4, idDoc);
            stmt.executeUpdate();

            if (doc instanceof Book) {
                Book b = (Book) doc;
                String sqlBook = "UPDATE Book SET isbn = ?, pageNumber = ? WHERE id_doc = ?";
                PreparedStatement stmtBook = conn.prepareStatement(sqlBook);
                stmtBook.setString(1, newIsbn != null ? newIsbn : b.getIsbn());
                stmtBook.setObject(2, newPageNumber != null ? newPageNumber : b.getPageNumber());
                stmtBook.setInt(3, idDoc);
                stmtBook.executeUpdate();
            } else if (doc instanceof Magazine) {
                Magazine m = (Magazine) doc;
                String sqlMag = "UPDATE Magazine SET number = ?, periodicity = ? WHERE id_doc = ?";
                PreparedStatement stmtMag = conn.prepareStatement(sqlMag);
                stmtMag.setObject(1, newNumber != null ? newNumber : m.getNumber());
                stmtMag.setString(2, newPeriodicity != null ? newPeriodicity.name() : m.getPeriodicity().name());
                stmtMag.setInt(3, idDoc);
                stmtMag.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateDocument(Document doc) {
        if (doc == null) throw new DocumentNotFoundException("Cannot update: document is null");

        String sqlDoc = "UPDATE Document SET title = ?, author = ?, genre = ?, available = ? WHERE id_doc = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sqlDoc)) {
            stmt.setString(1, doc.getTitle());
            stmt.setString(2, doc.getAuthor());
            stmt.setString(3, doc.getGenre());
            stmt.setBoolean(4, doc.isAvailable());
            stmt.setInt(5, doc.getIdDoc());

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DocumentNotFoundException("Document not found in DB: id=" + doc.getIdDoc());
            }

            // Update child tables only if necessary
            if (doc instanceof Book b) {
                String sqlBook = "UPDATE Book SET isbn = ?, pageNumber = ? WHERE id_doc = ?";
                try (PreparedStatement stmtBook = conn.prepareStatement(sqlBook)) {
                    stmtBook.setString(1, b.getIsbn());
                    stmtBook.setInt(2, b.getPageNumber());
                    stmtBook.setInt(3, b.getIdDoc());
                    stmtBook.executeUpdate();
                }
            } else if (doc instanceof Magazine m) {
                String sqlMag = "UPDATE Magazine SET number = ?, periodicity = ? WHERE id_doc = ?";
                try (PreparedStatement stmtMag = conn.prepareStatement(sqlMag)) {
                    stmtMag.setInt(1, m.getNumber());
                    stmtMag.setString(2, m.getPeriodicity().name());
                    stmtMag.setInt(3, m.getIdDoc());
                    stmtMag.executeUpdate();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to update document", e);
        }
    }

    // -------------------- REMOVE --------------------
    public void removeDocument(Document document) {
        try {
            int idDoc = getIdDocForDocument(document);

            if (document instanceof Book) {
                String sqlBook = "DELETE FROM Book WHERE id_doc = ?";
                PreparedStatement stmtBook = conn.prepareStatement(sqlBook);
                stmtBook.setInt(1, idDoc);
                stmtBook.executeUpdate();
            } else if (document instanceof Magazine) {
                String sqlMag = "DELETE FROM Magazine WHERE id_doc = ?";
                PreparedStatement stmtMag = conn.prepareStatement(sqlMag);
                stmtMag.setInt(1, idDoc);
                stmtMag.executeUpdate();
            }

            String sqlDoc = "DELETE FROM Document WHERE id_doc = ?";
            PreparedStatement stmtDoc = conn.prepareStatement(sqlDoc);
            stmtDoc.setInt(1, idDoc);
            stmtDoc.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -------------------- HELPERS --------------------

    private Document getFullDocumentById(int idDoc) throws SQLException {
        // Check Book
        String sqlBook = "SELECT * FROM Book WHERE id_doc = ?";
        PreparedStatement stmtBook = conn.prepareStatement(sqlBook);
        stmtBook.setInt(1, idDoc);
        ResultSet rsBook = stmtBook.executeQuery();
        if (rsBook.next()) {
            return new Book(
                rsBook.getString("title"),
                rsBook.getString("author"),
                rsBook.getString("genre"),
                rsBook.getString("isbn"),
                (Integer) rsBook.getObject("pageNumber")
            );
        }

        // Check Magazine
        String sqlMag = "SELECT * FROM Magazine WHERE id_doc = ?";
        PreparedStatement stmtMag = conn.prepareStatement(sqlMag);
        stmtMag.setInt(1, idDoc);
        ResultSet rsMag = stmtMag.executeQuery();
        if (rsMag.next()) {
            return new Magazine(
                rsMag.getString("title"),
                rsMag.getString("author"),
                rsMag.getString("genre"),
                (Integer) rsMag.getObject("number"),
                Periodicity.valueOf(rsMag.getString("periodicity"))
            );
        }

        throw new SQLException("Document type not found for id_doc = " + idDoc);
    }

    private int getIdDocForDocument(Document doc) throws SQLException {
        String sql = "SELECT id_doc FROM Document WHERE title = ? AND author = ? AND genre = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, doc.getTitle());
        stmt.setString(2, doc.getAuthor());
        stmt.setString(3, doc.getGenre());
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) return rs.getInt("id_doc");
        throw new SQLException("Document not found in Document table.");
    }
    public boolean isDocumentBorrowed(int idDoc) {
        String sql = "SELECT COUNT(*) FROM Borrow WHERE id_doc = ? AND return_date IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idDoc);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // true if there is an active borrow
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}