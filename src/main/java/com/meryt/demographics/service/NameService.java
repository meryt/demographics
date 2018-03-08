package com.meryt.demographics.service;

import java.util.Collections;
import java.util.Set;
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
        return nameRepository.randomFirstName(gender, Collections.emptySet());
    }

    public String randomFirstName(Gender gender, Set<String> excludeNames) {
        return nameRepository.randomFirstName(gender, excludeNames);
    }

    public String randomLastName() {
        return nameRepository.randomLastName();
    }

    public String randomName(Gender gender) {
        return nameRepository.randomFirstName(gender, Collections.emptySet()) + " " + nameRepository.randomLastName();
    }

}
