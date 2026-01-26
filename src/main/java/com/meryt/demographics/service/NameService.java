package com.meryt.demographics.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.springframework.util.StringUtils;

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
        Set<String> cultures = null;
        if (StringUtils.hasText(culture)) {
            cultures = Arrays.stream(culture.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
        }
        return nameRepository.randomFirstName(gender, excludeNames == null ? Collections.emptySet() : excludeNames,
                onDate, cultures);
    }

    /**
     * Get a random last name, optionally filtered by culture.
     *
     * @param culture optionally, a comma-separated list of cultures to filter by
     * @return a name
     */
    @NonNull
    public String randomLastName(@Nullable String culture) {
        Set<String> cultures = null;
        if (StringUtils.hasText(culture)) {
            cultures = Arrays.stream(culture.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
        }
        return nameRepository.randomLastName(cultures);
    }

    /**
     * Return a random name for the gender, optionally filtered by date and culture.
     *
     * @param gender the gender (required)
     * @param onDate optionally, a date to limit the name options available
     * @param culture optionally, a comma-separated list of cultures to filter by
     * @return a name
     */
    @NonNull
    public String randomName(@NonNull Gender gender, @Nullable LocalDate onDate, @Nullable String culture) {
        Set<String> cultures = null;
        if (StringUtils.hasText(culture)) {
            cultures = Arrays.stream(culture.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
        }
        return nameRepository.randomFirstName(gender,
                Collections.emptySet(), onDate, cultures) + " " + nameRepository.randomLastName(cultures);
    }

}
