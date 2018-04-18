package com.meryt.demographics.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonOccupationPeriod;
import com.meryt.demographics.domain.person.PersonTitlePeriod;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.HouseholdInhabitantPeriod;

/**
 * This response shows details about a person and descends into its member family, households, and occupations.
 */
@Getter
public class PersonDetailResponse extends PersonResponse {

    private final List<PersonTitleResponse> titles;
    private final List<PersonFamilyResponse> families;
    private final List<PersonOccupationResponse> occupations;
    private final HouseholdResponse household;
    private final List<HouseholdResponse> households;
    private final PersonParentsFamilyResponse family;

    public PersonDetailResponse(@NonNull Person person) {
        this(person, null);
    }

    public PersonDetailResponse(@NonNull Person person, @Nullable LocalDate onDate) {
        super(person);

        if (person.getTitles().isEmpty()) {
            titles = null;
        } else {
            titles = new ArrayList<>();
            for (PersonTitlePeriod titlePeriod : person.getTitles()) {
                titles.add(new PersonTitleResponse(titlePeriod));
            }
        }

        if (person.getFamilies().isEmpty()) {
            families = null;
        } else {
            families = new ArrayList<>();
            for (Family fam : person.getFamilies()) {
                families.add(new PersonFamilyResponse(person, fam));
            }
        }

        if (person.getOccupations().isEmpty()) {
            occupations = null;
        } else {
            occupations = new ArrayList<>();
            for (PersonOccupationPeriod occupation : person.getOccupations()) {
                occupations.add(new PersonOccupationResponse(occupation));
            }
        }

        family = person.getFamily() == null ? null : new PersonParentsFamilyResponse(person.getFamily(), person);

        if (onDate != null) {
            households = null;
            Household personHousehold = person.getHousehold(onDate);
            household = personHousehold == null ? null : new HouseholdResponse(personHousehold, onDate);

        } else {
            household = null;
            households = new ArrayList<>();
            for (HouseholdInhabitantPeriod householdPeriod : person.getHouseholds()) {
                households.add(new HouseholdResponse(householdPeriod.getHousehold()));
            }
        }
    }
}
