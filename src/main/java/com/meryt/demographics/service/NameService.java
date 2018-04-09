package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nullable;

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

    public String randomFirstName(Gender gender, @Nullable LocalDate onDate) {
        return nameRepository.randomFirstName(gender, Collections.emptySet(), onDate);
    }

    public String randomFirstName(Gender gender, Set<String> excludeNames, @Nullable LocalDate onDate) {
        return nameRepository.randomFirstName(gender, excludeNames, onDate);
    }

    public String randomLastName() {
        return nameRepository.randomLastName();
    }

    public String randomName(Gender gender) {
        return nameRepository.randomFirstName(gender, Collections.emptySet(), null) + " " + nameRepository.randomLastName();
    }

}
