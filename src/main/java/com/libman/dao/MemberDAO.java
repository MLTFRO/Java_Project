package com.libman.dao;

import com.libman.model.Member;
import com.libman.model.PenaltyStatus;
import com.libman.model.Borrow;
import com.libman.exception.MemberNotFoundException;

import java.util.List;

public interface MemberDAO {
    void addMember(Member member);
    Member searchMemberById(int memberId) throws MemberNotFoundException;
    Member searchMemberByName(String name, String surname) throws MemberNotFoundException;
    void updateMember(Member member, String name, String surname, PenaltyStatus penaltyStatus);
    List<Borrow> getMemberHistory(Member member);
    PenaltyStatus hasPenalty(Member member);
    List<Member> getAllMembers();
    void deleteMember(int memberId);
}
