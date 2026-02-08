package com.meryt.demographics.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.Trait;

/**
 * This response shows details about a person without descending into members. It is ideal for showing the person as
 * a member of a family or household.
 */
@Getter
public class PersonResponse extends PersonSummaryResponse {

    private final String middleName;
    private final Gender gender;
    private final String birthPlace;
    private final String deathPlace;
    private final String causeOfDeath;
    private final String ageAtDeath;

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
    private boolean storyCharacter;
    private Integer mainCharacter;

    private final List<String> traits;


    public PersonResponse(@NonNull Person person) {
        this(person, null);
    }

    public PersonResponse(@NonNull Person person, @Nullable LocalDate onDate) {
        super(person, onDate);
        gender = person.getGender();
        middleName = person.getMiddleNames();
        birthPlace = person.getBirthPlace();
        deathPlace = person.getDeathPlace();
        ageAtDeath = person.getAgeAtDeath();
        causeOfDeath = person.getCauseOfDeath();

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

        storyCharacter = person.isStoryCharacter();
        mainCharacter = person.getMainCharacter();
    }

}
