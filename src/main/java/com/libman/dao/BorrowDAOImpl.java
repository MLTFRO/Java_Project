package com.libman.dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.libman.model.Book;
import com.libman.model.Borrow;
import com.libman.model.Document;
import com.libman.model.Magazine;
import com.libman.model.Member;
import com.libman.exception.BorrowException;
import com.libman.exception.DocumentNotFoundException;
import com.libman.exception.MemberNotFoundException;
import com.libman.dao.*;



public class BorrowDAOImpl implements BorrowDAO {

    private Connection conn;

    public BorrowDAOImpl() {
        this.conn = DatabaseManager.getConnection();
    }

    @Override
    public boolean addBorrow(Borrow borrow) throws SQLException {
        if (borrow == null || borrow.getDocument() == null || borrow.getMember() == null) {
            throw new IllegalArgumentException("Borrow, Document, and Member must not be null");
        }

        // Check if Document exists
        String checkDocSql = "SELECT 1 FROM Document WHERE id_doc = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkDocSql)) {
            stmt.setInt(1, borrow.getDocument().getIdDoc());
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                throw new SQLException("Document id " + borrow.getDocument().getIdDoc() + " does not exist!");
            }
        }

        // Check if Member exists
        String checkMemberSql = "SELECT 1 FROM Member WHERE idMember = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkMemberSql)) {
            stmt.setInt(1, borrow.getMember().getIdMember());
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                throw new SQLException("Member id " + borrow.getMember().getIdMember() + " does not exist!");
            }
        }

        // Generate new Borrow ID (BR001, BR002, ...)
        String newId = "BR001";
        String getMaxIdSql = "SELECT id FROM Borrow ORDER BY id DESC LIMIT 1";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(getMaxIdSql)) {
            if (rs.next()) {
                String lastId = rs.getString("id"); // e.g., BR005
                int number = Integer.parseInt(lastId.substring(2)) + 1;
                newId = String.format("BR%03d", number);
            }
        }

        // Insert borrow into DB
        String insertSql = "INSERT INTO Borrow (id, id_doc, idMember, borrowDate, expectedReturnDate, returnDate) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, newId);
            stmt.setInt(2, borrow.getDocument().getIdDoc());
            stmt.setInt(3, borrow.getMember().getIdMember());

            // Convert LocalDate to java.sql.Date for SQLite
            if (borrow.getBorrowDate() != null)
                stmt.setDate(4, java.sql.Date.valueOf(borrow.getBorrowDate()));
            else
                stmt.setNull(4, java.sql.Types.DATE);

            if (borrow.getExpectedReturnDate() != null)
                stmt.setDate(5, java.sql.Date.valueOf(borrow.getExpectedReturnDate()));
            else
                stmt.setNull(5, java.sql.Types.DATE);

            if (borrow.getReturnDate() != null)
                stmt.setDate(6, java.sql.Date.valueOf(borrow.getReturnDate()));
            else
                stmt.setNull(6, java.sql.Types.DATE);

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new SQLException("Error adding borrow to database: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeBorrow(Borrow borrow) throws BorrowException {
        if (borrow == null) throw new BorrowException("Borrow is null");

        try {
            // Update the borrow to set returnDate instead of deleting
            String sql = "UPDATE Borrow SET returnDate = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, LocalDate.now().toString());
            stmt.setString(2, borrow.getId());
            int rows = stmt.executeUpdate();

            if (rows == 0) throw new BorrowException("Borrow not found");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new BorrowException("Failed to remove borrow: " + e.getMessage());
        }
    }

    @Override
    public List<Borrow> getCurrentBorrows() {
        return getBorrows("SELECT * FROM Borrow WHERE returnDate IS NULL");
    }

    @Override
    public List<Borrow> getLateBorrows() {
        return getBorrows("SELECT * FROM Borrow WHERE returnDate IS NULL AND expectedReturnDate < ?");
    }

    @Override
    public List<Borrow> getAllBorrows() {
        return getBorrows("SELECT * FROM Borrow");
    }

    @Override
    public boolean isDocumentBorrowed(int idDoc) {
        String sql = "SELECT COUNT(*) FROM Borrow WHERE id_doc = ? AND returnDate IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idDoc);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;  // If count > 0, document is borrowed
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check if document is borrowed", e);
        }
        return false;
    }

    // ------------------ Helper Methods ------------------

    private List<Borrow> getBorrows(String sql) {
        List<Borrow> borrows = new ArrayList<>();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (sql.contains("?")) {
                stmt.setString(1, LocalDate.now().toString());
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                borrows.add(mapResultSetToBorrow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return borrows;
    }

    private Borrow mapResultSetToBorrow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        int docId = rs.getInt("id_doc");
        int memberId = rs.getInt("idMember");

        // Use existing DAO methods to fetch objects
        Document doc = new DocumentDAOImpl().getDocumentById(docId);
        Member member = new MemberDAOImpl().searchMemberById(memberId); 

        LocalDate borrowDate = rs.getString("borrowDate") != null ? LocalDate.parse(rs.getString("borrowDate")) : null;
        Borrow borrow = new Borrow(doc, member, borrowDate);
        borrow.setId(id);

        LocalDate expectedReturn = rs.getString("expectedReturnDate") != null
                ? LocalDate.parse(rs.getString("expectedReturnDate")) : null;
        borrow.setExpectedReturnDate(expectedReturn);

        LocalDate returnDate = rs.getString("returnDate") != null
                ? LocalDate.parse(rs.getString("returnDate")) : null;
        borrow.setReturnDate(returnDate);

        return borrow;
    }

    public List<Borrow> getLateBorrowsForMember(int memberId, MemberDAO memberDAO, DocumentDAO documentDAO) throws Exception {
        List<Borrow> lateBorrows = new ArrayList<>();
        String sql = "SELECT * FROM Borrow WHERE idMember = ? AND returnDate IS NULL AND expectedReturnDate < ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, memberId);
            stmt.setString(2, java.time.LocalDate.now().toString());

            ResultSet rs = stmt.executeQuery();
            Member member = memberDAO.searchMemberById(memberId); // fetch member once

            while (rs.next()) {
                int docId = rs.getInt("id_doc");
                Document doc = documentDAO.getDocumentById(docId); // fetch document

                String borrowDateStr = rs.getString("borrowDate");
                LocalDate borrowDate = borrowDateStr != null ? LocalDate.parse(borrowDateStr) : LocalDate.now();

                Borrow borrow = new Borrow(doc, member, borrowDate);

                String returnDateStr = rs.getString("returnDate");
                if (returnDateStr != null) {
                    borrow.setReturnDate(LocalDate.parse(returnDateStr));
                }

                String expectedReturnStr = rs.getString("expectedReturnDate");
                if (expectedReturnStr != null) {
                    borrow.setExpectedReturnDate(LocalDate.parse(expectedReturnStr));
                }

                borrow.setId(rs.getString("id"));
                lateBorrows.add(borrow);
            }
        }

        return lateBorrows;
    }

    @Override
    public int countActiveBorrowsForMember(int memberId) throws Exception {
        String sql = "SELECT COUNT(*) FROM Borrow WHERE idMember = ? AND returnDate IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
            return 0;
        }
    }
}