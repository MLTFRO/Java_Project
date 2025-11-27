package com.libman.dao;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.libman.model.Magazine;
import com.libman.exception.DocumentNotFoundException;

public class MagazineDAOImpl implements MagazineDAO {

    private Connection conn;

    public MagazineDAOImpl() {
        this.conn = DatabaseManager.getConnection();
    }

    @Override
    public void addMagazine(Magazine magazine) throws DocumentNotFoundException {

        String insertDocumentSQL = 
            "INSERT INTO Document (title, author, genre) VALUES (?, ?, ?)";

        String insertMagazineSQL = 
            "INSERT INTO Magazine (title, number, periodicity, author, genre, id_doc) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            // 1️⃣ Insert into Document table
            PreparedStatement stmtDoc = conn.prepareStatement(insertDocumentSQL, Statement.RETURN_GENERATED_KEYS);
            stmtDoc.setString(1, magazine.getTitle());
            stmtDoc.setString(2, magazine.getAuthor());
            stmtDoc.setString(3, magazine.getGenre());
            stmtDoc.executeUpdate();

            // 2️⃣ Retrieve generated id_doc
            ResultSet rs = stmtDoc.getGeneratedKeys();
            if (!rs.next()) {
                throw new DocumentNotFoundException("Failed to retrieve generated id_doc.");
            }
            int id_doc = rs.getInt(1);
            magazine.setIdDoc(id_doc);

            // 3️⃣ Insert into Magazine table
            PreparedStatement stmtMag = conn.prepareStatement(insertMagazineSQL);
            stmtMag.setString(1, magazine.getTitle());
            stmtMag.setInt(2, magazine.getNumber());
            stmtMag.setString(3, magazine.getPeriodicity().name());
            stmtMag.setString(4, magazine.getAuthor());
            stmtMag.setString(5, magazine.getGenre());
            stmtMag.setInt(6, id_doc);

            stmtMag.executeUpdate();

            System.out.println("Magazine added with id_doc=" + id_doc);

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DocumentNotFoundException("Failed to add magazine: " + e.getMessage());
        }
    }

    @Override
    public Magazine getMagazineByNumber(int number) throws DocumentNotFoundException {
        try {
            String sql = "SELECT m.title AS mag_title, m.number, m.periodicity, " +
                         "d.id_doc, d.author, d.genre " +
                         "FROM Magazine m " +
                         "JOIN Document d ON m.id_doc = d.id_doc " +
                         "WHERE m.number = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, number);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Magazine mag = new Magazine(
                    rs.getString("mag_title"),
                    rs.getString("author"),
                    rs.getString("genre"),
                    rs.getInt("number"),
                    Magazine.Periodicity.valueOf(rs.getString("periodicity"))
                );
                mag.setIdDoc(rs.getInt("id_doc"));
                return mag;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        throw new DocumentNotFoundException("Magazine with number " + number + " not found");
    }

    @Override
    public void updateMagazineAttributes(Magazine magazine,
                                         String newTitle,
                                         String newAuthor,
                                         String newGenre,
                                         Integer newNumber,
                                         Magazine.Periodicity newPeriodicity) throws DocumentNotFoundException {
        String sqlDoc = "UPDATE Document SET title = ?, author = ?, genre = ? WHERE id_doc = ?";
        String sqlMag = "UPDATE Magazine SET number = ?, periodicity = ? WHERE id_doc = ?";

        try {
            // Update Document table
            PreparedStatement stmtDoc = conn.prepareStatement(sqlDoc);
            stmtDoc.setString(1, newTitle != null ? newTitle : magazine.getTitle());
            stmtDoc.setString(2, newAuthor != null ? newAuthor : magazine.getAuthor());
            stmtDoc.setString(3, newGenre != null ? newGenre : magazine.getGenre());
            stmtDoc.setInt(4, magazine.getIdDoc());
            stmtDoc.executeUpdate();

            // Update Magazine table
            PreparedStatement stmtMag = conn.prepareStatement(sqlMag);
            stmtMag.setInt(1, newNumber != null ? newNumber : magazine.getNumber());
            stmtMag.setString(2, newPeriodicity != null ? newPeriodicity.name() : magazine.getPeriodicity().name());
            stmtMag.setInt(3, magazine.getIdDoc());
            stmtMag.executeUpdate();

            // Update object fields
            if (newTitle != null) magazine.setTitle(newTitle);
            if (newAuthor != null) magazine.setAuthor(newAuthor);
            if (newGenre != null) magazine.setGenre(newGenre);
            if (newNumber != null) magazine.setNumber(newNumber);
            if (newPeriodicity != null) magazine.setPeriodicity(newPeriodicity);

            System.out.println("Magazine updated: id_doc=" + magazine.getIdDoc());

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DocumentNotFoundException("Failed to update magazine: " + e.getMessage());
        }
    }

    @Override
    public void removeMagazine(Magazine magazine) throws DocumentNotFoundException {
        String sqlMag = "DELETE FROM Magazine WHERE id_doc = ?";
        String sqlDoc = "DELETE FROM Document WHERE id_doc = ?";

        try {
            // Delete from Magazine table first
            PreparedStatement stmtMag = conn.prepareStatement(sqlMag);
            stmtMag.setInt(1, magazine.getIdDoc());
            stmtMag.executeUpdate();

            // Delete from Document table
            PreparedStatement stmtDoc = conn.prepareStatement(sqlDoc);
            stmtDoc.setInt(1, magazine.getIdDoc());
            stmtDoc.executeUpdate();

            System.out.println("Magazine removed: id_doc=" + magazine.getIdDoc());

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DocumentNotFoundException("Failed to remove magazine: " + e.getMessage());
        }
    }

    @Override
    public List<Magazine> getAllMagazines() {
        List<Magazine> magazines = new ArrayList<>();
        String sql = "SELECT m.title AS mag_title, m.number, m.periodicity, " +
                     "d.id_doc, d.author, d.genre " +
                     "FROM Magazine m " +
                     "JOIN Document d ON m.id_doc = d.id_doc";

        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Magazine mag = new Magazine(
                    rs.getString("mag_title"),
                    rs.getString("author"),
                    rs.getString("genre"),
                    rs.getInt("number"),
                    Magazine.Periodicity.valueOf(rs.getString("periodicity"))
                );
                mag.setIdDoc(rs.getInt("id_doc"));
                magazines.add(mag);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve magazines: " + e.getMessage(), e);
        }

        return magazines;
    }
}