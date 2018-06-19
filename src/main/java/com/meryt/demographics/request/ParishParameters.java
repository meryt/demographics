package com.meryt.demographics.request;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

import com.meryt.demographics.domain.place.Region;
import com.meryt.demographics.generator.random.Die;

@Getter
@Setter
public class ParishParameters {

    private double squareMiles = 100.0;
    private long populationPerSquareMile = 40;
    private long minTownPopulation = 50;
    private boolean persist;
    private Long locationId;
    private Region location;
    private String parishName;
    private List<String> townNames;
    private List<String> estateNames;
    private List<String> estateSuffixes;
    private RandomFamilyParameters familyParameters;

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

    /**
     * Gets and removes a random name from the estates list, if any, otherwise a random town name. Randomly may add
     * one of the random estate suffixes, if any. (But does not remove these from the list.)
     *
     * @return a string containing an estate name, or null if there was no data to use for generating one.
     */
    @Nullable
    public String getAndRemoveRandomEstateName() {
        String baseName;
        if (getEstateNames() == null || getEstateNames().isEmpty()) {
            baseName = getAndRemoveRandomTownName();
        } else {
            baseName = estateNames.remove(new Die(getEstateNames().size()).roll() - 1);
        }

        if (estateSuffixes == null || estateSuffixes.isEmpty()) {
            return baseName;
        }

        int index = new Die(getEstateSuffixes().size() + 2).roll() - 1;
        if (index >= getEstateSuffixes().size()) {
            return baseName;
        }
        String suffix = estateSuffixes.get(index);
        return baseName + " " + suffix;
    }
}
