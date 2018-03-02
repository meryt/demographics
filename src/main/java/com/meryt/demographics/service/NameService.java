package com.meryt.demographics.service;

import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.repository.NameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NameService {

    private final NameRepository nameRepository;

    public NameService(@Autowired NameRepository nameRepository) {
        this.nameRepository = nameRepository;
    }

    public String randomFirstName(Gender gender) {
        return nameRepository.randomFirstName(gender);
    }

    public String randomLastName() {
        return nameRepository.randomLastName();
    }

    public String randomName(Gender gender) {
        return nameRepository.randomFirstName(gender) + " " + nameRepository.randomLastName();
    }

}
