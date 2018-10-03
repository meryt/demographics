package com.meryt.demographics.repository;

import java.time.LocalDate;
import java.util.List;
import lombok.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


import com.meryt.demographics.domain.place.Household;

@Repository
public interface HouseholdRepository extends CrudRepository<Household, Long> {

    @Query(value = "SELECT * FROM households h " +
            "INNER JOIN household_locations hl ON h.id = hl.household_id " +
            "    AND hl.from_date <= :onDate " +
            "    AND (hl.to_date IS NULL OR hl.to_date > :onDate) " +
            "INNER JOIN dwelling_places dp ON hl.dwelling_place_id = dp.id AND dp.dwelling_place_type != 'DWELLING'",
            nativeQuery =  true)
    List<Household> findHouseholdsWithoutHouses(@NonNull LocalDate onDate);

    @Query(value = "SELECT * FROM households h " +
            "LEFT JOIN household_locations hl ON h.id = hl.household_id " +
            "    AND hl.from_date <= :onDate " +
            "    AND (hl.to_date IS NULL OR hl.to_date > :onDate) " +
            "WHERE hl.dwelling_place_id IS NULL",
            nativeQuery =  true)
    List<Household> findHouseholdsWithoutLocations(@NonNull LocalDate onDate);

    @Query(value = "SELECT \n" +
            "    h.*\n" +
            "FROM (  SELECT household_id, MAX(to_date) \n" +
            "        FROM household_inhabitants \n" +
            "        GROUP BY 1 \n" +
            "        HAVING MAX(to_date) < :onDate) y \n" +
            "INNER JOIN household_locations hl \n" +
            "    ON y.household_id = hl.household_id AND DATERANGE(hl.from_date, hl.to_date) @> CAST(:onDate AS DATE) \n" +
            "INNER JOIN households h ON y.household_id = h.id\n",
            nativeQuery =  true)
    List<Household> loadHouseholdsWithoutInhabitantsInLocations(@NonNull LocalDate onDate);
}
