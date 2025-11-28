package com.libman.dao;

import java.sql.*;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import com.libman.model.Borrow;
import com.libman.model.Document;
import com.libman.model.Member;
import com.libman.exception.BorrowException;

public class BorrowDAOImpl implements BorrowDAO {

    private Connection conn;
    private DocumentDAO documentDAO;
    private MemberDAO memberDAO;

    public BorrowDAOImpl() {
        this.conn = DatabaseManager.getConnection();
        this.documentDAO = new DocumentDAOImpl();
        this.memberDAO = new MemberDAOImpl();
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
                String lastId = rs.getString("id");
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

            if (borrow.getBorrowDate() != null)
                stmt.setString(4, borrow.getBorrowDateString());
            else
                stmt.setNull(4, java.sql.Types.DATE);

            if (borrow.getExpectedReturnDate() != null)
                stmt.setString(5, borrow.getExpectedReturnDateString());
            else
                stmt.setNull(5, java.sql.Types.DATE);

            if (borrow.getReturnDate() != null)
                stmt.setString(6, borrow.getReturnDateString());
            else
                stmt.setNull(6, java.sql.Types.DATE);

            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                borrow.setId(newId);
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            throw new SQLException("Error adding borrow to database: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeBorrow(Borrow borrow) throws BorrowException {
        if (borrow == null) throw new BorrowException("Borrow is null");
        if (borrow.getId() == null || borrow.getId().isEmpty()) {
            throw new BorrowException("Borrow ID is null or empty");
        }

        try {
            // Set return date to mark as returned
            String updateSql = "UPDATE Borrow SET returnDate = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, LocalDate.now().toString());
                stmt.setString(2, borrow.getId());
                int rows = stmt.executeUpdate();

                if (rows == 0) throw new BorrowException("Borrow not found with ID: " + borrow.getId());
            }
            
            // Update document availability to true (don't use 'available' column)
            if (borrow.getDocument() != null && borrow.getDocument().getIdDoc() > 0) {
                Document doc = documentDAO.getDocumentById(borrow.getDocument().getIdDoc());
                if (doc != null) {
                    doc.setAvailability(true);
                    // Just update in memory, availability is calculated from Borrow table
                }
            }
            
            // Update member's borrow count
            if (borrow.getMember() != null && borrow.getMember().getIdMember() > 0) {
                Member member = memberDAO.searchMemberById(borrow.getMember().getIdMember());
                if (member != null) {
                    member.setNbBorrows(Math.max(0, member.getNbBorrows() - 1));
                    memberDAO.updateMember(member, member.getName(), member.getSurname(), member.getPenaltyStatus());
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            throw new BorrowException("Failed to return document: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new BorrowException("Failed to update member/document: " + e.getMessage());
        }
    }

    /**
     * Permanently delete a borrow record from the database
     */
    public void deleteBorrow(String borrowId) throws BorrowException {
        if (borrowId == null || borrowId.isEmpty()) {
            throw new BorrowException("Borrow ID is null or empty");
        }

        try {
            // First, get the borrow to update document and member
            String selectSql = "SELECT id_doc, idMember, returnDate FROM Borrow WHERE id = ?";
            int docId = 0;
            int memberId = 0;
            boolean wasReturned = false;
            
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setString(1, borrowId);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    docId = rs.getInt("id_doc");
                    memberId = rs.getInt("idMember");
                    wasReturned = rs.getDate("returnDate") != null;
                } else {
                    throw new BorrowException("Borrow not found with ID: " + borrowId);
                }
            }

            // If not returned yet, update member's borrow count
            if (!wasReturned && memberId > 0) {
                Member member = memberDAO.searchMemberById(memberId);
                if (member != null) {
                    member.setNbBorrows(Math.max(0, member.getNbBorrows() - 1));
                    memberDAO.updateMember(member, member.getName(), member.getSurname(), member.getPenaltyStatus());
                }
            }

            // Now delete the borrow record
            String deleteSql = "DELETE FROM Borrow WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setString(1, borrowId);
                int rows = stmt.executeUpdate();
                
                if (rows == 0) {
                    throw new BorrowException("Failed to delete borrow with ID: " + borrowId);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            throw new BorrowException("Database error while deleting borrow: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new BorrowException("Error deleting borrow: " + e.getMessage());
        }
    }

    /**
     * Delete a borrow using the Borrow object
     */
    public void deleteBorrow(Borrow borrow) throws BorrowException {
        if (borrow == null || borrow.getId() == null) {
            throw new BorrowException("Borrow or Borrow ID is null");
        }
        deleteBorrow(borrow.getId());
    }

    @Override
    public List<Borrow> getCurrentBorrows() {
        return getBorrows("SELECT * FROM Borrow WHERE returnDate IS NULL", null);
    }

    @Override
    public List<Borrow> getLateBorrows() {
        return getBorrows("SELECT * FROM Borrow WHERE returnDate IS NULL AND expectedReturnDate < date('now')", null);
    }

    @Override
    public List<Borrow> getAllBorrows() {
        return getBorrows("SELECT * FROM Borrow", null);
    }

    @Override
    public boolean isDocumentBorrowed(int idDoc) {
        String sql = "SELECT COUNT(*) FROM Borrow WHERE id_doc = ? AND returnDate IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idDoc);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check if document is borrowed", e);
        }
        return false;
    }

    // ------------------ Helper Methods ------------------

    private List<Borrow> getBorrows(String sql, java.sql.Date dateParam) {
        List<Borrow> borrows = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (dateParam != null) {
                stmt.setDate(1, dateParam);
            }
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    Borrow borrow = mapResultSetToBorrow(rs);
                    if (borrow != null) {
                        borrows.add(borrow);
                    }
                } catch (Exception e) {
                    System.err.println("Error mapping borrow: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching borrows: " + e.getMessage());
            e.printStackTrace();
        }
        return borrows;
    }

    private Borrow mapResultSetToBorrow(ResultSet rs) throws SQLException {
        try {
            String id = rs.getString("id");
            int docId = rs.getInt("id_doc");
            int memberId = rs.getInt("idMember");

            // Fetch document and member using shared DAO instances
            Document doc = documentDAO.getDocumentById(docId);
            Member member = memberDAO.searchMemberById(memberId);

            // Verify we got valid objects
            if (doc == null) {
                System.err.println("Document not found for id: " + docId);
                return null;
            }
            if (member == null) {
                System.err.println("Member not found for id: " + memberId);
                return null;
            }

            // Parse dates safely - handle both timestamps and date strings
            LocalDate borrowDate = parseDate(rs, "borrowDate");
            LocalDate expectedReturn = parseDate(rs, "expectedReturnDate");
            LocalDate returnDate = parseDate(rs, "returnDate");

            // CRITICAL: Use default constructor to avoid null pointer
            Borrow borrow = new Borrow();
            borrow.setId(id);
            borrow.setDocument(doc);
            borrow.setMember(member);
            borrow.setBorrowDate(borrowDate != null ? borrowDate : LocalDate.now());
            borrow.setExpectedReturnDate(expectedReturn != null ? expectedReturn : LocalDate.now().plusDays(14));
            borrow.setReturnDate(returnDate);

            return borrow;
            
        } catch (Exception e) {
            System.err.println("Error in mapResultSetToBorrow: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("Failed to map borrow from database", e);
        }
    }

    private LocalDate parseDate(ResultSet rs, String columnName) throws SQLException {
        try {
            // First, try to get as SQL Date
            Date sqlDate = rs.getDate(columnName);
            if (sqlDate != null) {
                return sqlDate.toLocalDate();
            }
        } catch (SQLException e) {
            // If that fails, try other methods
        }
        
        try {
            // Try as string
            String dateStr = rs.getString(columnName);
            if (dateStr != null && !dateStr.isEmpty()) {
                // Check if it's a timestamp (all digits)
                if (dateStr.matches("\\d+")) {
                    // Convert milliseconds timestamp to LocalDate
                    long timestamp = Long.parseLong(dateStr);
                    return Instant.ofEpochMilli(timestamp)
                                  .atZone(ZoneId.systemDefault())
                                  .toLocalDate();
                } else {
                    // Try parsing as ISO date string
                    return LocalDate.parse(dateStr);
                }
            }
        } catch (Exception ex) {
            System.err.println("Failed to parse date from column '" + columnName + "': " + rs.getString(columnName));
        }
        
        return null;
    }

    public List<Borrow> getLateBorrowsForMember(int memberId, MemberDAO memberDAO, DocumentDAO documentDAO) throws Exception {
        List<Borrow> lateBorrows = new ArrayList<>();
        String sql = "SELECT * FROM Borrow WHERE idMember = ? AND returnDate IS NULL AND expectedReturnDate < date('now')";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, memberId);

            ResultSet rs = stmt.executeQuery();
            Member member = memberDAO.searchMemberById(memberId);

            while (rs.next()) {
                int docId = rs.getInt("id_doc");
                Document doc = documentDAO.getDocumentById(docId);

                LocalDate borrowDate = parseDate(rs, "borrowDate");
                if (borrowDate == null) borrowDate = LocalDate.now();

                Borrow borrow = new Borrow();
                borrow.setDocument(doc);
                borrow.setMember(member);
                borrow.setBorrowDate(borrowDate);
                borrow.setReturnDate(parseDate(rs, "returnDate"));
                borrow.setExpectedReturnDate(parseDate(rs, "expectedReturnDate"));
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