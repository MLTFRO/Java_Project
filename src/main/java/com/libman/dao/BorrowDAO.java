package com.libman.dao;

import java.util.List;
import com.libman.model.Borrow;
import com.libman.exception.BorrowException;
import java.sql.SQLException;

public interface BorrowDAO {
    boolean addBorrow(Borrow borrow) throws SQLException;
    void removeBorrow(Borrow borrow) throws BorrowException;
    List<Borrow> getCurrentBorrows();
    List<Borrow> getLateBorrows();
    List<Borrow> getAllBorrows();  // ADD THIS LINE
    boolean isDocumentBorrowed(int idDoc);
    List<Borrow> getLateBorrowsForMember(int memberId, MemberDAO memberDAO, DocumentDAO documentDAO) throws Exception;
    int countActiveBorrowsForMember(int memberId) throws Exception;
}