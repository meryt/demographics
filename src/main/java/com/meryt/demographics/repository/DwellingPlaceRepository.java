package com.meryt.demographics.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.meryt.demographics.domain.place.DwellingPlace;
import com.meryt.demographics.domain.place.DwellingPlaceType;

public interface DwellingPlaceRepository extends PagingAndSortingRepository<DwellingPlace, Long>, CrudRepository<DwellingPlace, Long> {

    List<DwellingPlace> findByType(@NonNull DwellingPlaceType type);

    List<DwellingPlace> findByTypeIn(@NonNull Set<DwellingPlaceType> types);

    Page<DwellingPlace> findByTypeIn(@NonNull Set<DwellingPlaceType> types, Pageable pageable);

    List<DwellingPlace> findByParentIsNotNullAndAttachedToParentIsTrue();

    List<DwellingPlace> findByParentIsNull();

    List<DwellingPlace> findByName(@NonNull String name);

    /**
     * Find houses that are not entailed and not attached to a parent, which have had no occupants for 30 years
     * @param onDate the date on which to check
     * @return a list of 0 or more dwellings
     */
    @Query(value = "SELECT dp.*\n" +
            "FROM dwelling_places dp\n" +
            "INNER JOIN (\n" +
            "    SELECT\n" +
            "        hl.dwelling_place_id\n" +
            "    FROM household_locations hl\n" +
            "    INNER JOIN dwelling_places dp ON hl.dwelling_place_id = dp.id\n" +
            "    WHERE dp.dwelling_place_type = 'DWELLING'\n" +
            "    AND NOT dp.attached_to_parent\n" +
            "    AND NOT dp.entailed\n" +
            "    AND dp.entailed_title_id IS NULL\n" +
            "    AND dp.ruined_date IS NULL\n" +
            "    AND dp.founded_date < CAST(:onDate AS DATE)\n" +
            "    GROUP BY 1\n" +
            "    HAVING MAX(COALESCE(hl.to_date, '2100-01-01')) < '2100-01-01' \n" +
            "        AND ((CAST(:onDate AS DATE) - CAST(MAX(hl.to_date) AS DATE)) / 365) > :yearsBeforeRuined \n" +
            ") AS y ON y.dwelling_place_id = dp.id",
            nativeQuery =  true)
    List<DwellingPlace> findDerelictHouses(@NonNull LocalDate onDate, int yearsBeforeRuined);
}
