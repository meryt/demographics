package com.meryt.demographics.domain.family;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.time.FormatPeriod;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * A Family consists of a male and female and optionally any biological children. The two people may or may not be
 * married.
 */
@Getter
@Setter
public class Family {

    private Person husband;
    private Person wife;
    private List<Person> children;
    private LocalDate weddingDate;

    public String getHusbandAgeAtMarriage() {
        if (husband == null || husband.getBirthDate() == null || weddingDate == null) {
            return null;
        }
        return FormatPeriod.diffAsYearsMonthsDays(husband.getBirthDate(), weddingDate);
    }

    public String getWifeAgeAtMarriage() {
        if (wife == null || wife.getBirthDate() == null || weddingDate == null) {
            return null;
        }
        return FormatPeriod.diffAsYearsMonthsDays(wife.getBirthDate(), weddingDate);
    }

    /**
     * Adds a person as either a husband or wife depending on their gender.
     *
     * @throws IllegalStateException if a spouse of this gender is already present.
     */
    public void addSpouse(@NonNull Person spouse) {
        if ((spouse.isFemale() && wife != null) || (spouse.isMale() && husband != null)) {
            throw new IllegalStateException("Cannot add spouse; a spouse of this gender already belongs to family.");
        }
        if (spouse.isMale()) {
            husband = spouse;
        } else {
            wife = spouse;
        }
    }
}
