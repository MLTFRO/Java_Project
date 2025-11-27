package com.libman.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.time.temporal.ChronoUnit;
import com.libman.exception.*;

public class LibraryManager{
    private List<Member> members = new ArrayList<>();
    private List<Document> documents = new ArrayList<>();
    private List<Borrow> borrows = new ArrayList<>();
    private static final int MAX_BORROWS_PER_MEMBER = 5;
    private static final double PENALTY_PER_DAY = 0.5;

    //Members
    public void addMember(Member member){
        members.add(member);
    }

    public Member searchMemberById(int id){
        for(Member m : members){
            if(m.getIdMember() == id){
                return m;
            }
        }
        throw new MemberNotFoundException("No member found with id = " + id);
    }

    public Member searchMemberByName(String name, String surname){
        for(Member m : members){
            if((m.getName().equalsIgnoreCase(name))&&(m.getSurname().equalsIgnoreCase(surname))){
                return(m);
            }
        }
        return(null);
    }

    public void updateMember(Member member, String name, String surname, PenaltyStatus penaltyStatus){
        member.setName(name);
        member.setSurname(surname);
        member.setPenaltyStatus(penaltyStatus);
    }

    public List<Borrow> getMemberHistory(Member member){
        List<Borrow> history = new ArrayList<>();
        for(Borrow b: borrows){
            if(b.getMember().equals(member)){
                history.add(b);
            }
        }
        return(history);
    }

    public PenaltyStatus hasPenalty(Member member){
        return(member.getPenaltyStatus());
    }

    //Documents
    public void addDocument(Document document){
        documents.add(document);
    }

    public Document getDocumentByTitle(String title){
        for(Document d : documents){
            if(d.getTitle().equalsIgnoreCase(title)){
                return(d);
            }
        }
        return(null);
    }

    public Document getDocumentByAuthor(String author){
        for(Document d : documents){
            if(d.getAuthor().equalsIgnoreCase(author)){
                return(d);
            }
        }
        return(null);
    }

    public Document getDocumentByGenre(String genre){
        for(Document d : documents){
            if(d.getTitle().equalsIgnoreCase(genre)){
                return(d);
            }
        }
        return(null);
    }

    public Book getBookByIsbn(String isbn) {
        for (Document d : documents) {
            if (d instanceof Book) {
                Book b = (Book) d;
                if (b.getIsbn().equals(isbn)) {
                    return b;
                }
            }
        }
        throw new DocumentNotFoundException("No book found with ISBN = " + isbn);
    }

    public void updateDocumentAttributes(Document doc,
                                        String newTitle,
                                        String newAuthor,
                                        String newGenre,
                                        String newIsbn,
                                        Integer newPageNumber,
                                        Integer newNumber,
                                        Magazine.Periodicity newPeriodicity) {

        if (doc == null)
            throw new DocumentNotFoundException("Cannot update: document is null");

        if (newTitle != null) doc.setTitle(newTitle);
        if (newAuthor != null) doc.setAuthor(newAuthor);
        if (newGenre != null) doc.setGenre(newGenre);

        if(doc instanceof Book){
            Book b = (Book) doc;

            if(newIsbn != null) b.setIsbn(newIsbn);
            if(newPageNumber != null){
                if(newPageNumber <= 0) throw new IllegalArgumentException("Page count must be > 0");
                b.setPageNumber(newPageNumber);
            }

        } else if (doc instanceof Magazine){
            Magazine m = (Magazine) doc;

            if(newNumber != null){
                if(newNumber <= 0) throw new IllegalArgumentException("Magazine number must be > 0");
                m.setNumber(newNumber);
            }
            if(newPeriodicity != null) m.setPeriodicity(newPeriodicity);

        } else {
            throw new IllegalArgumentException("Unknown document type.");
        }
    }

    public void removeDocument(Document document){
        documents.remove(document);
    }

    //Borrows

    public boolean addBorrow(Member member, Document document) {

        if (member == null)
            throw new MemberNotFoundException("Member is null.");

        if (document == null)
            throw new DocumentNotFoundException("Document is null.");

        if (!document.isAvailable())
            throw new BorrowException("Document is not available.");

        if (member.getNbBorrows() >= MAX_BORROWS_PER_MEMBER)
            throw new BorrowException("This member cannot borrow more documents.");

        if (member.getPenaltyStatus().getLevel() >= 2)
            throw new BorrowException("This member is suspended or banned.");

        for (Borrow b : borrows) {
            if (b.getMember().equals(member) &&
                b.getReturnDate() == null &&
                b.getExpectedReturnDate().isBefore(LocalDate.now())) {

                throw new BorrowException("Member has overdue items.");
            }
        }

        Borrow borrow = new Borrow(document, member, LocalDate.now());
        borrows.add(borrow);

        document.setAvailability(false);
        member.setNbBorrows(member.getNbBorrows() + 1);

        return true;
    }

    public void removeBorrow(Borrow borrow){
        if (borrow == null)
            throw new BorrowException("Borrow is null.");

        LocalDate today = LocalDate.now();

        if (borrow.getReturnDate() != null)
            throw new BorrowException("This borrow has already been returned.");

        if (borrow.getExpectedReturnDate().isBefore(today)) {
            long delay = ChronoUnit.DAYS.between(borrow.getExpectedReturnDate(), today);
            double penalty = delay * PENALTY_PER_DAY;
            borrow.getMember().setPenalty(borrow.getMember().getPenalty() + penalty);
        }

        borrow.getDocument().setAvailability(true);
        borrow.getMember().setNbBorrows(borrow.getMember().getNbBorrows() - 1);

        borrow.setReturnDate(today);
    }

    public List<Borrow> getCurrentBorrows(){
        return(borrows);
    }

    public List<Borrow> getLateBorrows(){
        List<Borrow> toReturn = new ArrayList<>();
        for(Borrow b : borrows){
            if(b.getExpectedReturnDate().isBefore(LocalDate.now())){
                toReturn.add(b);
            }
        }
        return(toReturn);
    }
}