import dao.MemberDAO;
import dao.MemberDAOImpl;
import dao.DocumentDAO;
import dao.DocumentDAOImpl;
import dao.BookDAO;
import dao.BookDAOImpl;
import dao.BorrowDAO;
import dao.BorrowDAOImpl;
import dao.LibraryManagerDAO;

import model.Member;
import model.Book;
import model.Document;
import model.PenaltyStatus;

import exception.MemberNotFoundException;
import exception.DocumentNotFoundException;

public class TestLibrary {

    public static void main(String[] args) {
        try {
            // DAOs
            MemberDAO memberDAO = new MemberDAOImpl();
            DocumentDAO documentDAO = new DocumentDAOImpl();
            BookDAO bookDAO = new BookDAOImpl();
            BorrowDAO borrowDAO = new BorrowDAOImpl();

            LibraryManagerDAO manager = new LibraryManagerDAO(memberDAO, documentDAO, bookDAO, borrowDAO);

            // ----------------------------
            // 1) Add Member (unique ID)
            // ----------------------------
            int memberId = 200; // unique ID for testing
            Member m1 = new Member(memberId, "Alice", "Smith", PenaltyStatus.NONE);

            // Check if member exists
            Member savedMember;
            try {
                savedMember = memberDAO.searchMemberById(memberId);
                System.out.println("Member already exists: " + savedMember.getName());
            } catch (MemberNotFoundException e) {
                memberDAO.addMember(m1);
                savedMember = memberDAO.searchMemberById(memberId);
                System.out.println("✔ Member added: " + savedMember.getName());
            }

            // ----------------------------
            // 2) Add Book (unique ISBN)
            // ----------------------------
            String isbn = "987-098-1-56-132"; // ensure unique ISBN
            Book b1;

            try {
                Document existingDoc = bookDAO.getBookByIsbn(isbn);
                System.out.println("Book already exists: " + existingDoc.getTitle());
                b1 = (Book) existingDoc;
            } catch (DocumentNotFoundException e) {
                // Create new book
                b1 = new Book("The Picture of Dorian Gray", "Oscar Wilde", "Classics", isbn, 282);

                // Add the book (inserts Document + Book)
                bookDAO.addBook(b1);

                // Retrieve saved book to get correct idDoc
                b1 = (Book) bookDAO.getBookByIsbn(isbn);
                System.out.println("✔ Book added: '" + b1.getTitle() + "' with id_doc=" + b1.getIdDoc());
            }

            // ----------------------------
            // 3) Add Borrow (ensure uniqueness by id_doc only)
            // ----------------------------
            final int bookIdDoc = b1.getIdDoc(); // must be final for lambda
            boolean alreadyBorrowed = manager.getMemberHistory(savedMember).stream()
                    .anyMatch(b -> b.getDocument().getIdDoc() == bookIdDoc);
            if (alreadyBorrowed) {
                System.out.println("⚠ Member already has this book borrowed (id_doc=" + bookIdDoc + ") and not returned.");
            } else {
                manager.addBorrow(savedMember, b1);
                System.out.println("✔ Borrow added successfully!");
            }
            // ----------------------------
            // 4) Show member history
            // ----------------------------
            System.out.println(manager.getMemberHistory(savedMember));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
