package com.meryt.demographics.generator;

import com.meryt.demographics.domain.place.Realm;
import com.meryt.demographics.domain.place.RuralArea;
import com.meryt.demographics.domain.place.Town;
import com.meryt.demographics.request.RealmParameters;

public class RealmGenerator {

    private final Die d4 = new Die(4);

    public Realm generate(RealmParameters realmParameters) {
        Realm realm = new Realm();
        realm.setAreaSquareMiles(realmParameters.getSquareMiles());

        long totalPopulation = realmParameters.getPopulation();

        int townIndex = 1;
        Town town1 = new Town();
        town1.setName("Town " + townIndex++);
        long lastPopulation = largestTownPopulation(totalPopulation);
        town1.setPopulation(lastPopulation);
        realm.getDwellingPlaces().add(town1);

        while ((totalPopulation - realm.getPopulation()) > realmParameters.getMinTownPopulation()
                && lastPopulation > realmParameters.getMinTownPopulation()) {
            if (townIndex == 2) {
                lastPopulation = secondTownPopulation(lastPopulation);
            } else {
                lastPopulation = furtherTownPopulation(lastPopulation);
            }
            Town town = new Town();
            town.setName("Town " + townIndex++);
            town.setPopulation(lastPopulation);

            realm.getDwellingPlaces().add(town);
        }

        long remainingPopulation = totalPopulation - realm.getPopulation();
        RuralArea ruralArea = new RuralArea();
        ruralArea.setPopulation(remainingPopulation);
        ruralArea.setName("Rural areas");
        realm.getDwellingPlaces().add(ruralArea);

        return realm;
    }

    /**
     * The largest town is equal to (P times M), where P is equal to the square root of the country's population,
     * and M is equal to a random roll of 2d4+10
     */
    private long largestTownPopulation(long totalPopulation) {
        double p = Math.sqrt(totalPopulation);
        int m = d4.roll(2) + 10;
        return Math.round(p * m);
    }

    private long secondTownPopulation(long largestTownPopulation) {
        double percent = d4.roll(2) * 0.1;
        return Math.round(largestTownPopulation * percent);
    }

    private long furtherTownPopulation(long lastTownPopulation) {
        double percent = d4.roll(2) * 0.05;
        long pop = Math.round(lastTownPopulation * percent);
        return pop == 0 ? 1 : pop;
    }

}
