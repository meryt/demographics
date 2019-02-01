package com.meryt.demographics.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.rest.BadRequestException;
import com.meryt.demographics.rest.ConflictException;
import com.meryt.demographics.rest.ResourceNotFoundException;

@Service
public class ControllerHelperService {

    private final ConfigurationService configurationService;
    private final PersonService personService;
    private final DwellingPlaceService dwellingPlaceService;

    public ControllerHelperService(@NonNull @Autowired ConfigurationService configurationService,
                                   @NonNull @Autowired PersonService personService,
                                   @NonNull @Autowired DwellingPlaceService dwellingPlaceService) {
        this.configurationService = configurationService;
        this.personService = personService;
        this.dwellingPlaceService = dwellingPlaceService;
    }

    @Nullable
    public LocalDate parseDate(@Nullable String date) {
        if (StringUtils.isEmpty(date)) {
            return null;
        }
        if (date.equalsIgnoreCase("current")) {
            LocalDate currentDate = configurationService.getCurrentDate();
            if (currentDate == null) {
                throw new ConflictException("Unable to use current date: No current date is set in the database");
            }
            return currentDate;
        }

        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date: " + e.getMessage());
        }
    }

    @NonNull
    public Person loadPerson(Long personId) {
        if (personId == null) {
            throw new BadRequestException("person ID may not be null");
        }

        Person person = personService.load(personId);
        if (person == null) {
            throw new ResourceNotFoundException("No person found for ID " + personId);
        }
        return person;
    }

    @NonNull
    public DwellingPlace loadDwellingPlace(Long dwellingPlaceId) {
        if (dwellingPlaceId == null) {
            throw new BadRequestException("place ID may not be null");
        }
        DwellingPlace place = dwellingPlaceService.load(dwellingPlaceId);
        if (place == null) {
            throw new ResourceNotFoundException("No place found for ID " + dwellingPlaceId);
        }
        return place;
    }

}
