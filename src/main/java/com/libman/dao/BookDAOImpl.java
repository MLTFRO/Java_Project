package com.libman.dao;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.libman.model.Book;
import com.libman.exception.DocumentNotFoundException;

public class BookDAOImpl implements BookDAO {

    private Connection conn;

    public BookDAOImpl() {
        this.conn = DatabaseManager.getConnection();
    }

    @Override
    public void addBook(Book book) throws DocumentNotFoundException {

        String insertDocumentSQL = 
            "INSERT INTO Document (title, author, genre) VALUES (?, ?, ?)";

        String insertBookSQL = 
            "INSERT INTO Book (title, isbn, pageNumber, author, genre, id_doc) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            // 1️⃣ Insert into Document table first
            PreparedStatement stmtDoc = conn.prepareStatement(insertDocumentSQL, Statement.RETURN_GENERATED_KEYS);
            stmtDoc.setString(1, book.getTitle());
            stmtDoc.setString(2, book.getAuthor());
            stmtDoc.setString(3, book.getGenre());
            stmtDoc.executeUpdate();

            // 2️⃣ Retrieve generated id_doc
            ResultSet rs = stmtDoc.getGeneratedKeys();
            if (!rs.next()) {
                throw new DocumentNotFoundException("Failed to retrieve generated id_doc.");
            }
            int id_doc = rs.getInt(1);
            book.setIdDoc(id_doc); // Book inherits id from Document

            // 3️⃣ Insert into Book table
            PreparedStatement stmtBook = conn.prepareStatement(insertBookSQL);
            stmtBook.setString(1, book.getTitle());
            stmtBook.setString(2, book.getIsbn());
            stmtBook.setInt(3, book.getPageNumber());
            stmtBook.setString(4, book.getAuthor());
            stmtBook.setString(5, book.getGenre());
            stmtBook.setInt(6, id_doc);

            stmtBook.executeUpdate();

            System.out.println("Book added with id_doc=" + id_doc);

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DocumentNotFoundException("Failed to add book: " + e.getMessage());
        }
    }


    @Override
    public Book getBookByIsbn(String isbn) throws DocumentNotFoundException {
        try {
            // Join Book table with Document table to get common fields
            String sql = "SELECT b.title AS book_title, b.isbn AS isbn, b.pageNumber AS pageNumber, " +
                         "d.id_doc, d.author, d.genre " +
                         "FROM Book b " +
                         "JOIN Document d ON b.id_doc = d.id_doc " +
                         "WHERE b.isbn = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, isbn);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Book book = new Book(
                    rs.getString("book_title"),
                    rs.getString("author"),
                    rs.getString("genre"),
                    rs.getString("isbn"),
                    rs.getInt("pageNumber")
                );
                book.setIdDoc(rs.getInt("id_doc"));  // store the document ID for foreign key
                return book;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        throw new DocumentNotFoundException("Book with ISBN " + isbn + " not found");
    }

    @Override
    public void updateBookAttributes(Book book, String newTitle, String newAuthor, String newGenre,
                                    String newIsbn, Integer newPageNumber) throws DocumentNotFoundException {
        String sqlUpdateDocument = "UPDATE Document SET title = ?, author = ?, genre = ? WHERE id_doc = ?";
        String sqlUpdateBook = "UPDATE Book SET title = ?, isbn = ?, pageNumber = ? WHERE id_doc = ?";

        try {
            // 1️⃣ Update Document table
            PreparedStatement stmtDoc = conn.prepareStatement(sqlUpdateDocument);
            stmtDoc.setString(1, newTitle != null ? newTitle : book.getTitle());
            stmtDoc.setString(2, newAuthor != null ? newAuthor : book.getAuthor());
            stmtDoc.setString(3, newGenre != null ? newGenre : book.getGenre());
            stmtDoc.setInt(4, book.getIdDoc());
            int rowsUpdatedDoc = stmtDoc.executeUpdate();
            if (rowsUpdatedDoc == 0) {
                throw new DocumentNotFoundException("Book's document record not found for update.");
            }

            // 2️⃣ Update Book table
            PreparedStatement stmtBook = conn.prepareStatement(sqlUpdateBook);
            stmtBook.setString(1, newTitle != null ? newTitle : book.getTitle());
            stmtBook.setString(2, newIsbn != null ? newIsbn : book.getIsbn());
            stmtBook.setInt(3, newPageNumber != null ? newPageNumber : book.getPageNumber());
            stmtBook.setInt(4, book.getIdDoc());
            int rowsUpdatedBook = stmtBook.executeUpdate();
            if (rowsUpdatedBook == 0) {
                throw new DocumentNotFoundException("Book record not found for update.");
            }

            // ✅ Update local object
            if (newTitle != null) book.setTitle(newTitle);
            if (newAuthor != null) book.setAuthor(newAuthor);
            if (newGenre != null) book.setGenre(newGenre);
            if (newIsbn != null) book.setIsbn(newIsbn);
            if (newPageNumber != null) book.setPageNumber(newPageNumber);

            System.out.println("Book updated successfully: id_doc=" + book.getIdDoc());

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DocumentNotFoundException("Failed to update book: " + e.getMessage());
        }
    }

    @Override
    public void removeBook(Book book) throws DocumentNotFoundException {
        String sqlDeleteBook = "DELETE FROM Book WHERE id_doc = ?";
        String sqlDeleteDocument = "DELETE FROM Document WHERE id_doc = ?";

        try {
            // 1️⃣ Delete from Book table first
            PreparedStatement stmtBook = conn.prepareStatement(sqlDeleteBook);
            stmtBook.setInt(1, book.getIdDoc());
            int rowsDeletedBook = stmtBook.executeUpdate();
            if (rowsDeletedBook == 0) {
                throw new DocumentNotFoundException("Book record not found for deletion.");
            }

            // 2️⃣ Delete from Document table
            PreparedStatement stmtDoc = conn.prepareStatement(sqlDeleteDocument);
            stmtDoc.setInt(1, book.getIdDoc());
            int rowsDeletedDoc = stmtDoc.executeUpdate();
            if (rowsDeletedDoc == 0) {
                throw new DocumentNotFoundException("Document record not found for deletion.");
            }

            System.out.println("Book deleted successfully: id_doc=" + book.getIdDoc());

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DocumentNotFoundException("Failed to delete book: " + e.getMessage());
        }
    }

    @Override
    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT b.title AS book_title, b.isbn, b.pageNumber, " +
                    "d.id_doc, d.author, d.genre " +
                    "FROM Book b " +
                    "JOIN Document d ON b.id_doc = d.id_doc";

        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Book book = new Book(
                    rs.getString("book_title"),
                    rs.getString("author"),
                    rs.getString("genre"),
                    rs.getString("isbn"),
                    rs.getInt("pageNumber")
                );
                book.setIdDoc(rs.getInt("id_doc")); // store the document ID for reference
                books.add(book);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve books: " + e.getMessage(), e);
        }

        return books;
    }
}
