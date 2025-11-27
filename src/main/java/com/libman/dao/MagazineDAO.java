package com.libman.dao;

import com.libman.model.Magazine;
import com.libman.exception.DocumentNotFoundException;
import java.util.List;

public interface MagazineDAO {
    void addMagazine(Magazine magazine) throws DocumentNotFoundException;
    Magazine getMagazineByNumber(int number) throws DocumentNotFoundException;
    void updateMagazineAttributes(Magazine magazine,
                                  String newTitle,
                                  String newAuthor,
                                  String newGenre,
                                  Integer newNumber,
                                  Magazine.Periodicity newPeriodicity) throws DocumentNotFoundException;
    void removeMagazine(Magazine magazine) throws DocumentNotFoundException;

    // New method
    List<Magazine> getAllMagazines();
}