package com.meryt.demographics.service;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.repository.AncestryRepository;

@Service
public class AncestryService {

    private final AncestryRepository ancestryRepository;

    public AncestryService(@Autowired @NonNull AncestryRepository ancestryRepository) {
        this.ancestryRepository = ancestryRepository;
    }

    /**
     * Truncates and rebuilds the ancestry table in the database.
     */
    public void updateAncestryTable() {
        ancestryRepository.updateAncestryTable();
    }

}
