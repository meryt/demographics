package com.meryt.demographics.domain;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.SocialClass;

@Getter
@Setter
@Entity
@Table(name = "occupations")
public class Occupation {

    @Id
    @SequenceGenerator(name="occupations_id_seq", sequenceName="occupations_id_seq", allocationSize=1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="occupations_id_seq")
    private long id;

    private String name;

    private boolean allowMale;
    private boolean allowFemale;
    @Enumerated(EnumType.STRING)
    private SocialClass minClass;
    @Enumerated(EnumType.STRING)
    private SocialClass maxClass;
    private boolean isRural;
    private boolean isFarmOwner;
    private boolean isDomesticServant;
    private boolean isFarmLaborer;

    /**
     * How many of this occupation we expect to find per person in the population. Obviously this number is always less
     * than 1.
     */
    private double supportFactor;

    @Override
    public String toString() {
        return String.format("%d %s", id, name);
    }

    public List<SocialClass> getSocialClasses() {
        List<SocialClass> result = new ArrayList<>();
        for (int i = minClass.getRank(); i <= minClass.getRank(); i++) {
            result.add(SocialClass.fromRank(i));
        }
        return result;
    }

    /**
     * Gets all social classes from 1 up to the max rank for this occupation. This includes all social classes that
     * would be willing to do the job.
     */
    public List<SocialClass> getSocialClassesUpToMaxRank() {
        List<SocialClass> result = new ArrayList<>();
        for (int i = 1; i <= minClass.getRank(); i++) {
            result.add(SocialClass.fromRank(i));
        }
        return result;
    }

    /**
     * Gets the required gender for this occupation, or null if both genders can hold it
     */
    @Nullable
    public Gender getRequiredGender() {
        if (allowFemale && allowMale) {
            return null;
        }
        return allowFemale ? Gender.FEMALE : Gender.MALE;
    }
}
