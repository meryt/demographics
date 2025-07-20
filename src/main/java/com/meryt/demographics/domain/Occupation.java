package com.meryt.demographics.domain;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

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
    private boolean mayMarry;
    /**
     * Only valid for domestic servants, tells how much a household must earn before it can hire one of these.
     */
    private Double minIncomeRequired;
    /**
     * Only valid for domestic servants. Tells how many a household may hire if they earn at least the "max" income
     * for servants, namely 4000.
     */
    private int maxPerHousehold;

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
