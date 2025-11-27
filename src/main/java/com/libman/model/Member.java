package com.libman.model;

public class Member{
    private int idMember;
    private String name;
    private String surname;
    private PenaltyStatus penaltyStatus;
    private double penalty;
    private int nbBorrows;

    public Member(int idMember, String name, String surname, PenaltyStatus penaltyStatus) {
        this.idMember = idMember;
        this.name = name;
        this.surname = surname;
        this.penaltyStatus = penaltyStatus;
        this.penalty = 0.0;
        this.nbBorrows = 0;
    }

    public Member(int idMember, String name, String surname) {
        this.idMember = idMember;
        this.name = name;
        this.surname = surname;
        this.penaltyStatus = PenaltyStatus.NONE;
        this.nbBorrows = 0;
    }

    public PenaltyStatus getPenaltyStatus() { 
        return penaltyStatus;
    }
    public void setPenaltyStatus(PenaltyStatus status) { 
        this.penaltyStatus = status;
    }
    public double getPenalty(){
        return this.penalty;
    }
    public void setPenalty(double penalty){
        this.penalty = penalty;
    }

    public int getIdMember(){
        return this.idMember;
    }
    public String getName(){
        return this.name;
    }
    public String getSurname(){
        return this.surname;
    }

    public void setName(String name) { 
        this.name = name;
    }
    public void setSurname(String surname) { 
        this.surname = surname;
    }

    public void setIdMember(int idMember) {
        this.idMember = idMember;
    }


    public void setNbBorrows(int nbBorrows){
        this.nbBorrows = nbBorrows;
    }

    public int getNbBorrows(){
        return this.nbBorrows;
    }
}