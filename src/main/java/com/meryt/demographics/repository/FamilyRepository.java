package com.meryt.demographics.repository;

import java.time.LocalDate;
import java.util.List;
import lombok.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.family.Family;

@Repository
public interface FamilyRepository extends CrudRepository<Family, Long> {

    @Query(value =
            "SELECT\n" +
            "    f.*\n" +
            "FROM families f\n" +
            "INNER JOIN living_persons husbands ON f.husband_id = husbands.id\n" +
            "LEFT JOIN household_inhabitants hi1 ON husbands.id = hi1.person_id\n" +
            "  AND DATERANGE(hi1.from_date, hi1.to_date) @> CAST(:onDate AS DATE)\n" +
            "INNER JOIN living_persons wives ON f.wife_id = wives.id\n" +
            "LEFT JOIN household_inhabitants hi2 ON wives.id = hi2.person_id\n" +
            "    AND DATERANGE(hi2.from_date, hi2.to_date) @> CAST(:onDate AS DATE)\n" +
            "WHERE hi1.household_id IS DISTINCT FROM hi2.household_id",
            nativeQuery =  true)
    List<Family> loadFamiliesNotInSameHousehold(@NonNull LocalDate onDate);

}
