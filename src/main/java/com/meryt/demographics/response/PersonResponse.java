package com.meryt.demographics.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.Family;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonOccupationPeriod;
import com.meryt.demographics.domain.person.SocialClass;

@Getter
public class PersonResponse {

    private final long id;
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final Gender gender;
    private final LocalDate birthDate;
    private final String birthPlace;
    private final LocalDate deathDate;
    private final String deathPlace;
    private final String ageAtDeath;
    private final SocialClass socialClass;

    private final String eyeColor;
    private final String hairColor;

    private final double domesticity;
    private final double charisma;
    private final double comeliness;
    private final double intelligence;
    private final double morality;
    private final double strength;

    private final List<PersonFamilyResponse> families;
    private final List<PersonOccupationResponse> occupations;
    private final PersonParentsFamilyResponse family;

    public PersonResponse(@NonNull Person person) {
        id = person.getId();
        firstName = person.getFirstName();
        middleName = person.getMiddleNames();
        lastName = person.getLastName();
        gender = person.getGender();
        birthDate = person.getBirthDate();
        birthPlace = person.getBirthPlace();
        deathDate = person.getDeathDate();
        deathPlace = person.getDeathPlace();
        ageAtDeath = person.getAgeAtDeath();
        socialClass = person.getSocialClass();

        eyeColor = person.getEyeColorName();
        hairColor = person.getHairColor();

        domesticity = person.getDomesticity();
        charisma = person.getCharisma();
        comeliness = person.getComeliness();
        intelligence = person.getIntelligence();
        morality = person.getMorality();
        strength = person.getStrength();

        if (person.getFamilies().isEmpty()) {
            families = null;
        } else {
            families = new ArrayList<>();
            for (Family family : person.getFamilies()) {
                families.add(new PersonFamilyResponse(person, family));
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

    }

}
