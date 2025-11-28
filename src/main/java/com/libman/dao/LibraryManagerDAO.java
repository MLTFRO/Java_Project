package com.libman.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;

import com.libman.model.*;
import com.libman.exception.*;

public class LibraryManagerDAO {
    private MemberDAO memberDAO;
    private DocumentDAO documentDAO;
    private BookDAO bookDAO;
    private MagazineDAO magazineDAO;
    private BorrowDAO borrowDAO;

    private static final int MAX_BORROWS_PER_MEMBER = 5;
    private static final double PENALTY_PER_DAY = 0.5;

    public LibraryManagerDAO(MemberDAO memberDAO, DocumentDAO documentDAO,
                             BookDAO bookDAO, MagazineDAO magazineDAO, BorrowDAO borrowDAO) {
        this.memberDAO = memberDAO;
        this.documentDAO = documentDAO;
        this.bookDAO = bookDAO;
        this.magazineDAO = magazineDAO;
        this.borrowDAO = borrowDAO;
    }

    // -------------------- Members --------------------
    public void addMember(Member member) {
        memberDAO.addMember(member);
    }

    public int generateNextMemberId() {
        int id = 1;
        while (true) {
            try {
                memberDAO.searchMemberById(id);
                id++;
            } catch (MemberNotFoundException e) {
                return id;
            }
        }
    }

    public Member searchMemberById(Integer id) {
        try {
            return memberDAO.searchMemberById(id);
        } catch (MemberNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Member searchMemberByName(String name, String surname) {
        try {
            return memberDAO.searchMemberByName(name, surname);
        } catch (MemberNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public List<Member> getAllMembers() {
        return memberDAO.getAllMembers();
    }

    public void updateMember(Member member, String name, String surname, PenaltyStatus penaltyStatus) {
        memberDAO.updateMember(member, name, surname, penaltyStatus);
        member.setName(name);
        member.setSurname(surname);
        member.setPenaltyStatus(penaltyStatus);
    }

    public PenaltyStatus hasPenalty(Member member) {
        return member.getPenaltyStatus();
    }

    public List<Borrow> getMemberHistory(Member member) {
        return memberDAO.getMemberHistory(member);
    }

    public void deleteMember(Member member) {
        memberDAO.deleteMember(member.getIdMember());
    }

    public Document getDocumentByTitle(String title) throws SQLException {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be null or empty");
        }

        try {
            Document doc = documentDAO.getDocumentByTitle(title);
            if (doc == null) {
                throw new SQLException("Document with title '" + title + "' does not exist in the database");
            }
            return doc;
        } catch (DocumentNotFoundException e) {
            throw new SQLException("Document with title '" + title + "' not found", e);
        }
    }

    // -------------------- Books --------------------
    public void addBook(Book book) {
        try {
            bookDAO.addBook(book);
        } catch (DocumentNotFoundException e) {
            throw new RuntimeException("Failed to add book: " + e.getMessage(), e);
        }
    }

    public Book getBookByIsbn(String isbn) {
        try {
            return bookDAO.getBookByIsbn(isbn);
        } catch (DocumentNotFoundException e) {
            throw new RuntimeException("Book not found: " + e.getMessage(), e);
        }
    }

    /**
     * Update book - overloaded version that takes individual parameters
     */
    public void updateBook(Book book, String title, String author, String genre, String isbn, Integer pageNumber) {
        try {
            bookDAO.updateBookAttributes(book, title, author, genre, isbn, pageNumber);
            book.setTitle(title);
            book.setAuthor(author);
            book.setGenre(genre);
            book.setIsbn(isbn);
            book.setPageNumber(pageNumber);
        } catch (DocumentNotFoundException e) {
            throw new RuntimeException("Failed to update book: " + e.getMessage(), e);
        }
    }

    /**
     * Update book - simplified version that uses Book object's own properties
     */
    public void updateBook(Book book) {
        updateBook(book, book.getTitle(), book.getAuthor(), book.getGenre(), 
                   book.getIsbn(), book.getPageNumber());
    }

    /**
     * Delete book by ISBN
     */
    public void deleteBook(String isbn) {
        try {
            Book book = bookDAO.getBookByIsbn(isbn);
            bookDAO.removeBook(book);
        } catch (DocumentNotFoundException e) {
            throw new RuntimeException("Failed to delete book: " + e.getMessage(), e);
        }
    }

    public void removeBook(Book book) {
        try {
            bookDAO.removeBook(book);
        } catch (DocumentNotFoundException e) {
            throw new RuntimeException("Failed to remove book: " + e.getMessage(), e);
        }
    }

    public List<Book> getAllBooks() {
        return bookDAO.getAllBooks();
    }

    public List<Book> getBooksByAuthor(String author) {
        List<Book> result = new ArrayList<>();
        for (Book b : getAllBooks()) {
            if (b.getAuthor().equalsIgnoreCase(author)) result.add(b);
        }
        return result;
    }

    public List<Book> getBooksByGenre(String genre) {
        List<Book> result = new ArrayList<>();
        for (Book b : getAllBooks()) {
            if (b.getGenre().equalsIgnoreCase(genre)) result.add(b);
        }
        return result;
    }

    // -------------------- Magazines --------------------
    public void addMagazine(Magazine magazine) {
        try {
            magazineDAO.addMagazine(magazine);
        } catch (DocumentNotFoundException e) {
            throw new RuntimeException("Failed to add magazine: " + e.getMessage(), e);
        }
    }

    public Magazine getMagazineByNumber(int number) {
        try {
            return magazineDAO.getMagazineByNumber(number);
        } catch (DocumentNotFoundException e) {
            throw new RuntimeException("Magazine not found: " + e.getMessage(), e);
        }
    }

    /**
     * Update magazine - overloaded version that takes individual parameters
     */
    public void updateMagazine(Magazine magazine, String title, String author, String genre,
                               int number, Magazine.Periodicity periodicity) {
        try {
            magazineDAO.updateMagazineAttributes(magazine, title, author, genre, number, periodicity);
            magazine.setTitle(title);
            magazine.setAuthor(author);
            magazine.setGenre(genre);
            magazine.setNumber(number);
            magazine.setPeriodicity(periodicity);
        } catch (DocumentNotFoundException e) {
            throw new RuntimeException("Failed to update magazine: " + e.getMessage(), e);
        }
    }

    /**
     * Update magazine - simplified version that uses Magazine object's own properties
     */
    public void updateMagazine(Magazine magazine) {
        updateMagazine(magazine, magazine.getTitle(), magazine.getAuthor(), 
                      magazine.getGenre(), magazine.getNumber(), magazine.getPeriodicity());
    }

    /**
     * Delete magazine by number (accepts int)
     */
    public void deleteMagazine(int number) {
        try {
            Magazine magazine = magazineDAO.getMagazineByNumber(number);
            magazineDAO.removeMagazine(magazine);
        } catch (DocumentNotFoundException e) {
            throw new RuntimeException("Failed to delete magazine: " + e.getMessage(), e);
        }
    }

    public void removeMagazine(Magazine magazine) {
        try {
            magazineDAO.removeMagazine(magazine);
        } catch (DocumentNotFoundException e) {
            throw new RuntimeException("Failed to remove magazine: " + e.getMessage(), e);
        }
    }

    public List<Magazine> getAllMagazines() {
        return magazineDAO.getAllMagazines();
    }

    // -------------------- Documents (all types) --------------------

    /**
     * Returns a list of all documents (books + magazines)
     */
    public List<Document> getAllDocuments() {
        List<Document> all = new ArrayList<>();
        all.addAll(getAllBooks());
        all.addAll(getAllMagazines());
        return all;
    }

    /**
     * Search documents by type and query.
     * type: "All", "Title", "Author", "Genre", "ISBN"
     */
    public List<Document> searchDocuments(String type, String query) {
        List<Document> result = new ArrayList<>();

        if (type == null || type.equals("All") || type.equalsIgnoreCase("Title")) {
            for (Document d : getAllDocuments()) {
                if (d.getTitle().toLowerCase().contains(query.toLowerCase())) {
                    if (!result.contains(d)) result.add(d);
                }
            }
            if (!type.equalsIgnoreCase("All")) return result;
        }

        if (type == null || type.equals("All") || type.equalsIgnoreCase("Author")) {
            for (Document d : getAllDocuments()) {
                if (d.getAuthor().toLowerCase().contains(query.toLowerCase())) {
                    if (!result.contains(d)) result.add(d);
                }
            }
            if (!type.equalsIgnoreCase("All")) return result;
        }

        if (type == null || type.equals("All") || type.equalsIgnoreCase("Genre")) {
            for (Document d : getAllDocuments()) {
                if (d.getGenre().toLowerCase().contains(query.toLowerCase())) {
                    if (!result.contains(d)) result.add(d);
                }
            }
            if (!type.equalsIgnoreCase("All")) return result;
        }

        if (type == null || type.equals("All") || type.equalsIgnoreCase("ISBN")) {
            for (Document d : getAllBooks()) { // only books have ISBN
                Book b = (Book) d;
                if (b.getIsbn().toLowerCase().contains(query.toLowerCase())) {
                    if (!result.contains(b)) result.add(b);
                }
            }
        }

        return result;
    }

    // -------------------- Borrows --------------------
    public boolean addBorrow(Member member, Document document) throws SQLException {
        if (member == null) 
            throw new MemberNotFoundException("Member is null.");
        if (document == null) 
            throw new DocumentNotFoundException("Document is null.");

        // Fetch persisted member
        Member persistedMember = memberDAO.searchMemberById(member.getIdMember());
        if (persistedMember == null) 
            throw new MemberNotFoundException("Member does not exist in the database.");

        // Fetch persisted document
        Document persistedDocument = documentDAO.getDocumentById(document.getIdDoc());
        if (persistedDocument == null)
            throw new DocumentNotFoundException("Document does not exist in the database.");

        // Check availability and member limits
        if (!persistedDocument.isAvailable()) 
            throw new BorrowException("Document is not available.");
        if (persistedMember.getNbBorrows() >= MAX_BORROWS_PER_MEMBER)
            throw new BorrowException("This member cannot borrow more documents.");
        if (persistedMember.getPenaltyStatus().getLevel() >= 2)
            throw new BorrowException("This member is suspended or banned.");

        // Check overdue items
        for (Borrow b : memberDAO.getMemberHistory(persistedMember)) {
            if (b.getReturnDate() == null && b.getExpectedReturnDate().isBefore(LocalDate.now())) {
                throw new BorrowException("Member has overdue items.");
            }
        }

        // Create Borrow object with foreign keys properly set
        Borrow borrow = new Borrow();
        borrow.setIdDoc(persistedDocument.getIdDoc());
        borrow.setIdMember(persistedMember.getIdMember());
        borrow.setDocument(persistedDocument);
        borrow.setMember(persistedMember);
        borrow.setBorrowDate(LocalDate.now());
        borrow.setExpectedReturnDate(LocalDate.now().plusDays(14));
        borrow.setReturnDate(null);

        // Persist borrow via BorrowDAO
        boolean added = borrowDAO.addBorrow(borrow);
        if (!added)
            throw new BorrowException("Failed to add borrow to database.");

        // Update document availability
        persistedDocument.setAvailability(false);
        documentDAO.updateDocument(persistedDocument);

        // Update member's borrow count
        persistedMember.setNbBorrows(persistedMember.getNbBorrows() + 1);
        memberDAO.updateMember(
            persistedMember,
            persistedMember.getName(),
            persistedMember.getSurname(),
            persistedMember.getPenaltyStatus()
        );

        return true;
    }

    public void removeBorrow(Borrow borrow) {
        if (borrow == null) throw new BorrowException("Borrow is null.");
        if (borrow.getReturnDate() != null) throw new BorrowException("This borrow has already been returned.");

        LocalDate today = LocalDate.now();
        
        // Calculate penalty if late
        if (borrow.getExpectedReturnDate().isBefore(today)) {
            long delay = ChronoUnit.DAYS.between(borrow.getExpectedReturnDate(), today);
            double penalty = delay * PENALTY_PER_DAY;
            
            // Add penalty to member
            Member member = borrow.getMember();
            double newTotalPenalty = member.getPenalty() + penalty;
            member.setPenalty(newTotalPenalty);
            
            // Update penalty status based on total penalty
            PenaltyStatus newStatus = calculatePenaltyStatus(newTotalPenalty);
            member.setPenaltyStatus(newStatus);
            
            // Update member in database
            memberDAO.updateMember(member, member.getName(), member.getSurname(), newStatus);
        }

        // Mark document as available
        Document document = borrow.getDocument();
        document.setAvailability(true);
        documentDAO.updateDocument(document);
        
        // Decrease member's borrow count
        Member member = borrow.getMember();
        member.setNbBorrows(member.getNbBorrows() - 1);
        memberDAO.updateMember(member, member.getName(), member.getSurname(), member.getPenaltyStatus());

        // Set return date and update in database
        borrow.setReturnDate(today);
        borrowDAO.removeBorrow(borrow);
    }

    /**
     * Calculate penalty status based on total penalty amount
     */
    private PenaltyStatus calculatePenaltyStatus(double totalPenalty) {
        if (totalPenalty == 0) {
            return PenaltyStatus.NONE;
        } else if (totalPenalty < 10) {
            return PenaltyStatus.WARNING;
        } else if (totalPenalty < 50) {
            return PenaltyStatus.SUSPENDED;
        } else {
            return PenaltyStatus.BANNED;
        }
    }

    /**
     * Get all current (unreturned) borrows from the database
     */
    public List<Borrow> getCurrentBorrows() {
        return borrowDAO.getCurrentBorrows();
    }

    /**
     * Get all late (overdue) borrows from the database
     */
    public List<Borrow> getLateBorrows() {
        return borrowDAO.getLateBorrows();
    }

    /**
     * Get all borrows (both returned and unreturned)
     */
    public List<Borrow> getAllBorrows() {
        return borrowDAO.getAllBorrows();
    }

    /**
     * Check if a specific document is currently borrowed
     */
    public boolean isDocumentBorrowed(int idDoc) {
        return borrowDAO.isDocumentBorrowed(idDoc);
    }

    /**
     * Get borrows returned today for statistics
     */
    public int getReturnedTodayCount() {
        int count = 0;
        LocalDate today = LocalDate.now();
        
        for (Borrow b : borrowDAO.getAllBorrows()) {
            if (b.getReturnDate() != null && b.getReturnDate().equals(today)) {
                count++;
            }
        }
        return count;
    }

    public int getActiveBorrowsCount(int memberId) throws Exception {
        return borrowDAO.countActiveBorrowsForMember(memberId);
    }

    /**
     * Returns the total penalty for a given member based on overdue borrows.
     */
    public double getTotalPenaltyForMember(int memberId) throws Exception {
        List<Borrow> lateBorrows = borrowDAO.getLateBorrowsForMember(memberId, memberDAO, documentDAO);
        double totalPenalty = 0;
        for (Borrow b : lateBorrows) {
            long daysLate = ChronoUnit.DAYS.between(
                                b.getExpectedReturnDate(), LocalDate.now());
            if (daysLate > 0) {
                totalPenalty += daysLate * 0.5; // assuming $0.5 per day late
            }
        }
        return totalPenalty;
    }

    public DocumentDAO getDocumentDAO() {
        return documentDAO;
    }

    public MemberDAO getMemberDAO() {
        return memberDAO;
    }

    public BorrowDAO getBorrowDAO() {
        return borrowDAO;
    }
}