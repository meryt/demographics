package com.meryt.demographics.domain;

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
}
