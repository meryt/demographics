package com.meryt.demographics.response;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;

/**
 * This response shows details about a person without descending into members. It is ideal for showing the person as
 * a member of a family or household.
 */
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
    }

}
