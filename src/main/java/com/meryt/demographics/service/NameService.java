package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nullable;

import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.repository.NameRepository;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NameService {

    private final NameRepository nameRepository;

    public NameService(@Autowired NameRepository nameRepository) {
        this.nameRepository = nameRepository;
    }

    /**
     * Get a random first name.
     *
     * @param gender the gender (required)
     * @param excludeNames if non-null and non-empty, these names will not be allowed (e.g. use to prevent two living
     *                     siblings from having the same first name)
     * @param onDate optionally, a date to limit the name options available
     * @return a name
     */
    @NonNull
    public String randomFirstName(@NonNull Gender gender,
                                  @Nullable Set<String> excludeNames,
                                  @Nullable LocalDate onDate,
                                  @Nullable String culture) {
        return nameRepository.randomFirstName(gender, excludeNames == null ? Collections.emptySet() : excludeNames,
                onDate, culture);
    }

    public String randomLastName() {
        return nameRepository.randomLastName();
    }

    /**
     * Return a random name for the gender, across all time periods and cultures.
     *
     * @param gender the gender (required)
     * @return a name
     */
    @NonNull
    public String randomName(@NonNull Gender gender) {
        return nameRepository.randomFirstName(gender,
                Collections.emptySet(), null, null) + " " + nameRepository.randomLastName();
    }

}
