package com.meryt.demographics.request;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;

import com.meryt.demographics.generator.random.Die;

@Getter
@Setter
public class PlaceNameParameters {
    private List<String> townNames;
    private List<String> estateNames;
    private List<String> estateSuffixes;

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
    public Pair<String, String> getAndRemoveRandomEstateName() {
        String baseName;
        if (getEstateNames() == null || getEstateNames().isEmpty()) {
            baseName = getAndRemoveRandomTownName();
        } else {
            baseName = estateNames.remove(new Die(getEstateNames().size()).roll() - 1);
        }

        if (estateSuffixes == null || estateSuffixes.isEmpty()) {
            return Pair.of(baseName, null);
        }

        int index = new Die(getEstateSuffixes().size() + 2).roll() - 1;
        if (index >= getEstateSuffixes().size()) {
            return Pair.of(baseName, null);
        }
        String suffix = estateSuffixes.get(index);
        return Pair.of(baseName, suffix);
    }

    /**
     * Gets and removes a random farm name based on the estates list, if nonempty, otherwise based on a random town
     * name. The name always ends in "Farm"
     *
     * @return a string containing a farm name, or null if there was no data to use for generating one.
     */
    @Nullable
    public String getAndRemoveRandomFarmName() {
        String baseName;
        if (getEstateNames() == null || getEstateNames().isEmpty()) {
            baseName = getAndRemoveRandomTownName();
        } else {
            baseName = estateNames.remove(new Die(getEstateNames().size()).roll() - 1);
        }
        return baseName + " Farm";
    }
}
