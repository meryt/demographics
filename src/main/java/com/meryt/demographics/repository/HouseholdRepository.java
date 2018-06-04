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
            "    AND hl.from_date >= :onDate " +
            "    AND (hl.to_date IS NULL OR hl.to_date > :onDate) " +
            "INNER JOIN dwelling_places dp ON hl.dwelling_place_id = dp.id AND dp.dwelling_place_type != 'DWELLING'",
            nativeQuery =  true)
    List<Household> findHouseholdsWithoutHouses(@NonNull LocalDate onDate);
}
