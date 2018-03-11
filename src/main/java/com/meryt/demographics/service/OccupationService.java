package com.meryt.demographics.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meryt.demographics.domain.Occupation;
import com.meryt.demographics.generator.random.PercentDie;
import com.meryt.demographics.repository.OccupationRepository;

@Service
public class OccupationService {

    private final OccupationRepository occupationRepository;

    public OccupationService(@Autowired OccupationRepository occupationRepository) {
        this.occupationRepository = occupationRepository;
    }

    /**
     * Returns a list with the expected occupation slots for a town of this size. There is some randomness involved.
     *
     * @return a list which may contain duplicates if there is more than one slot for a given occupation.
     */
    public List<Occupation> occupationsForTownPopulation(long population) {
        PercentDie percentDie = new PercentDie();
        List<Occupation> results = new ArrayList<>();
        for (Occupation occupation : findAll()) {
            // The result of this calc will be a double like 3.56
            double totalOfType = occupation.getSupportFactor() * population;
            // So there are at least 3 of this type in the population
            int wholeNumber = (int) Math.floor(totalOfType);
            // And there is a 56% chance of another one
            double percent = totalOfType - wholeNumber;
            if (percentDie.roll() <= percent) {
                wholeNumber++;
            }
            // Add the occupation to the list once per time it appears
            for (int i = 0; i < wholeNumber; i++) {
                results.add(occupation);
            }
        }

        return results;
    }

    public Iterable<Occupation> findAll() {
        return occupationRepository.findAll();
    }
}
