package com.meryt.demographics.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonOccupationPeriod;
import com.meryt.demographics.domain.person.fertility.Maternity;
import com.meryt.demographics.domain.place.Household;
import com.meryt.demographics.domain.place.HouseholdInhabitantPeriod;
import com.meryt.demographics.domain.timeline.TimelineEntry;
import com.meryt.demographics.service.AncestryService;

/**
 * This response shows details about a person and descends into its member family, households, and occupations.
 */
@Getter
@JsonPropertyOrder({"id", "firstName", "lastName", "gender", "age", "birthDate", "deathDate", "ageAtDeath",
        "socialClass", "capital", "eyeColor", "hairColor", "height", "pregnancy", "currentHeight", "domesticity",
        "comeliness", "intelligence", "morality", "strength", "traits", "titles", "families", "occupations",
        "charisma", "household", "households", "ownedProperties", "family", "timeline"})
public class PersonDetailResponse extends PersonResponse {

    private final Double capital;
    private final List<PersonFamilyResponse> families;
    private final List<PersonOccupationResponse> occupations;
    private final OccupationReference occupation;
    private final HouseholdResponseWithLocations household;
    private final List<HouseholdResponseWithLocations> households;
    private final List<DwellingPlaceReference> ownedProperties;
    private final PersonParentsFamilyResponse family;
    private final List<PersonCapitalResponse> capitalHistory;
    private final List<ResidencePeriodResponse> residences;
    private final List<TimelineEntry> timeline;
    private final String pregnancy;

    public PersonDetailResponse(@NonNull Person person) {
        this(person, null, null);
    }

    public PersonDetailResponse(@NonNull Person person,
                                @Nullable LocalDate onDate,
                                @Nullable AncestryService ancestryService) {
        super(person, onDate);

        if (person.getFamilies().isEmpty()) {
            families = null;
        } else {
            families = new ArrayList<>();
            for (Family fam : person.getFamilies()) {
                families.add(new PersonFamilyResponse(person, fam, onDate));
            }
        }

        if (person.getOccupations().isEmpty()) {
            occupations = null;
            occupation = null;
        } else {
            if (onDate != null) {
                Occupation occ = person.getOccupation(onDate);
                if (occ != null) {
                    occupation = new OccupationReference(occ);
                } else {
                    occupation = null;
                }
            } else {
                occupation = null;
            }
            occupations = new ArrayList<>();
            for (PersonOccupationPeriod occupation : person.getOccupations()) {
                occupations.add(new PersonOccupationResponse(occupation));
            }
        }

        family = person.getFamily() == null ? null : new PersonParentsFamilyResponse(person.getFamily(), person, onDate);

        capitalHistory = person.getCapitalPeriods().stream()
                .map(PersonCapitalResponse::new)
                .collect(Collectors.toList());

        residences = person.getResidences().stream()
                .map(ResidencePeriodResponse::new)
                .collect(Collectors.toList());

        List<TimelineEntry> timelineEntries = person.getAllTimelineEntries();
        timeline = timelineEntries.isEmpty() ? null : timelineEntries;

        if (onDate != null) {
            capital = person.getCapital(onDate);

            households = null;
            Household personHousehold = person.getHousehold(onDate);
            household = personHousehold == null
                    ? null
                    : new HouseholdResponseWithLocations(personHousehold, onDate, ancestryService);

            List<DwellingPlaceReference> props = person.getOwnedDwellingPlaces(onDate).stream()
                .map(DwellingPlaceReference::new)
                .collect(Collectors.toList());
            if (props.isEmpty()) {
                ownedProperties = null;
            } else {
                ownedProperties = props;
            }

            if (person.isFemale()) {
                Maternity mat = person.getMaternity();
                if (mat.isPregnant(onDate)) {
                    if (mat.getMiscarriageDate() != null) {
                        pregnancy = String.format("Pregnant, will miscarry on %s", mat.getMiscarriageDate());
                    } else {
                        pregnancy = String.format("Pregnant, due on %s", mat.getDueDate());
                    }
                } else {
                    pregnancy = null;
                }
            } else {
                pregnancy = null;
            }

        } else {
            pregnancy = null;
            capital = null;
            household = null;
            households = new ArrayList<>();
            for (HouseholdInhabitantPeriod householdPeriod : person.getHouseholds()) {
                households.add(new HouseholdResponseWithLocations(householdPeriod.getHousehold(), null, ancestryService));
            }

            List<DwellingPlaceReference> props = person.getOwnedDwellingPlaces().stream()
                    .map(o -> new DwellingPlaceReference(o.getDwellingPlace()))
                    .collect(Collectors.toList());
            if (props.isEmpty()) {
                ownedProperties = null;
            } else {
                ownedProperties = props;
            }
        }
    }
}
