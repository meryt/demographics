package com.meryt.demographics.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.PersonTitlePeriod;
import com.meryt.demographics.domain.person.SocialClass;
import com.meryt.demographics.domain.person.Trait;

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
    private final String age;
    private final String deathPlace;
    private final String ageAtDeath;
    private final SocialClass socialClass;

    private final String eyeColor;
    private final String hairColor;
    private final String height;
    private final String currentHeight;

    private final double domesticity;
    private final double charisma;
    private final double comeliness;
    private final double intelligence;
    private final double morality;
    private final double strength;

    private final List<String> traits;
    private final List<PersonTitleResponse> titles;


    public PersonResponse(@NonNull Person person) {
        this(person, null);
    }

    public PersonResponse(@NonNull Person person, @Nullable LocalDate onDate) {
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
        height = person.getHeightString();
        if (onDate != null && person.isLiving(onDate)) {
            String heightOnDate = person.getHeightString(onDate);
            if (height != null && height.equals(heightOnDate)) {
                currentHeight = null;
            } else {
                currentHeight = heightOnDate;
            }
        } else {
            currentHeight = null;
        }

        domesticity = person.getDomesticity();
        charisma = person.getCharisma();
        comeliness = person.getComeliness();
        intelligence = person.getIntelligence();
        morality = person.getMorality();
        strength = person.getStrength();

        if (person.getTraits().isEmpty()) {
            traits = null;
        } else {
            traits = new ArrayList<>();
            for (Trait t : person.getTraits()) {
                traits.add(t.getName());
            }
        }

        if (person.getTitles().isEmpty()) {
            titles = null;
        } else {
            titles = new ArrayList<>();
            for (PersonTitlePeriod titlePeriod : person.getTitles()) {
                titles.add(new PersonTitleResponse(titlePeriod));
            }
        }

        if (onDate != null && person.isLiving(onDate)) {
            age = person.getAge(onDate);
        } else {
            age = null;
        }
    }

}
