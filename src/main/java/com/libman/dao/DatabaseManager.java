package com.libman.dao;

import java.sql.*;

public class DatabaseManager {

    private static final String URL = "jdbc:sqlite:Library.db";
    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(URL);
                connection.createStatement().execute("PRAGMA foreign_keys = ON");
                System.out.println("SQLite connection established!");

                initializeSchema();  // Creates tables if not exist
                populateInitialData(); // Populates Document, Member, Book, Magazine, Borrow tables

            } catch (SQLException e) {
                System.err.println("SQLite connection error: " + e.getMessage());
            }
        }
        return connection;
    }

    private static void initializeSchema() {
        try (Statement stmt = connection.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Document(
                    id_doc INTEGER PRIMARY KEY,
                    title TEXT,
                    author TEXT,
                    genre TEXT
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Member(
                    idMember INTEGER PRIMARY KEY,
                    name TEXT,
                    surname TEXT,
                    PenaltyStatus INTEGER CHECK(PenaltyStatus IN (0,1,2,3))
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Borrow (
                    id TEXT PRIMARY KEY,
                    id_doc INTEGER,
                    idMember INTEGER,
                    borrowDate TEXT,
                    expectedReturnDate TEXT,
                    returnDate TEXT,
                    FOREIGN KEY(id_doc) REFERENCES Document(id_doc),
                    FOREIGN KEY(idMember) REFERENCES Member(idMember)
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Book(
                    title TEXT,
                    isbn TEXT PRIMARY KEY,
                    pageNumber INTEGER,
                    author TEXT,
                    genre TEXT,
                    id_doc INTEGER,
                    FOREIGN KEY (id_doc) REFERENCES Document(id_doc)
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Magazine(
                    title TEXT,
                    id TEXT PRIMARY KEY,
                    number INTEGER,
                    periodicity TEXT CHECK(periodicity IN 
                        ('DAILY','WEEKLY','BIWEEKLY','MONTHLY','BIMONTHLY','QUARTERLY','YEARLY')),
                    author TEXT,
                    genre TEXT,
                    id_doc INTEGER,
                    FOREIGN KEY (id_doc) REFERENCES Document(id_doc)
                );
            """);

            System.out.println("Database schema ready.");

        } catch (SQLException e) {
            System.err.println("Schema creation error: " + e.getMessage());
        }
    }

    private static void populateInitialData() {
        try (Statement stmt = connection.createStatement()) {

            // --- Document ---
            stmt.executeUpdate("""
                INSERT OR IGNORE INTO Document (id_doc, title, author, genre) VALUES
                (1, 'Tech Today', 'Alice Martin', 'Technology'),
                (2, 'The Silent Dawn', 'Alice Martin', 'Fiction'),
                (3, 'Health Weekly', 'John Carter', 'Health'),
                (4, 'Whispers of Time', 'John Carter', 'Mystery'),
                (5, 'Global Insights', 'Sarah Lee', 'News'),
                (6, 'Echoes of the Past', 'Sarah Lee', 'Historical'),
                (7, 'Fashion Forward', 'Emma Wilson', 'Fashion'),
                (8, 'Shadows of Eternity', 'Emma Wilson', 'Fantasy'),
                (9, 'Science Now', 'Michael Smith', 'Science'),
                (10, 'Crimson Horizon', 'Michael Smith', 'Thriller');
            """);

            // --- Member ---
            stmt.executeUpdate("""
                INSERT OR IGNORE INTO Member (idMember, name, surname, PenaltyStatus) VALUES
                (101, 'Liam', 'Hughes', 0),
                (105, 'Olivia', 'Bennett', 1),
                (109, 'Noah', 'Foster', 0),
                (113, 'Ava', 'Murray', 2),
                (117, 'Lucas', 'Parker', 0);
            """);

            // --- Book ---
            stmt.executeUpdate("""
                INSERT OR IGNORE INTO Book (title, isbn, pageNumber, author, genre, id_doc) VALUES
                ('The Silent Dawn', '978-3-16-148410-0', 320, 'Alice Martin', 'Fiction', 2),
                ('Whispers of Time', '978-1-23-456789-7', 280, 'John Carter', 'Mystery', 4),
                ('Shadows of Eternity', '978-0-19-283398-3', 290, 'Emma Wilson', 'Fantasy', 8),
                ('Crimson Horizon', '978-0-14-044913-6', 360, 'Michael Smith', 'Thriller', 10);
            """);

            // --- Magazine ---
            stmt.executeUpdate("""
                INSERT OR IGNORE INTO Magazine (title, id, number, periodicity, author, genre, id_doc) VALUES
                ('Tech Today', 'M001', 101, 'DAILY', 'Alice Martin', 'Technology', 1),
                ('Health Weekly', 'M002', 45, 'WEEKLY', 'John Carter', 'Health', 3),
                ('Global Insights', 'M003', 12, 'MONTHLY', 'Sarah Lee', 'News', 5),
                ('Fashion Forward', 'M004', 30, 'MONTHLY', 'Emma Wilson', 'Fashion', 7),
                ('Science Now', 'M005', 78, 'WEEKLY', 'Michael Smith', 'Science', 9);
            """);

            // --- Borrow ---
            stmt.executeUpdate("""
                INSERT OR IGNORE INTO Borrow (id, id_doc, idMember, borrowDate, expectedReturnDate, returnDate) VALUES
                ('BR001', 2, 101, '2025-01-03', '2025-01-17', '2025-01-20'),
                ('BR002', 3, 105, '2025-01-05', '2025-01-19', NULL),
                ('BR003', 4, 109, '2025-01-07', '2025-01-21', '2025-01-18'),
                ('BR004', 8, 113, '2025-01-10', '2025-01-24', '2025-01-24'),
                ('BR005', 10, 117, '2025-01-11', '2025-01-25', '2025-01-30');
            """);

            System.out.println("Initial data populated successfully.");

        } catch (SQLException e) {
            System.err.println("Data population error: " + e.getMessage());
        }
    }
}
