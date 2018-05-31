package com.meryt.demographics.request;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.generator.random.Die;

@Getter
@Setter
public class ParishParameters {

    private double squareMiles = 100.0;
    private long populationPerSquareMile = 40;
    private long minTownPopulation = 50;
    private boolean persist;
    private String parishName;
    private List<String> townNames;
    private FamilyParameters familyParameters;

    public long getPopulation() {
        return Math.round(squareMiles * populationPerSquareMile);
    }

    /**
     * Gets a random entry from the townNames list, if non-empty. Removes this entry from the list, so that it won't
     * get used twice.
     *
     * @return a town name, or null if the list is null or empty
     */
    @Nullable
    public String getAndRemoveRandomTownName() {
        if (getTownNames() == null || getTownNames().isEmpty()) {
            return null;
        }

        int index = new Die(getTownNames().size()).roll() - 1;
        return townNames.remove(index);
    }
}
