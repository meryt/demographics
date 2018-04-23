package com.meryt.demographics.repository;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;

import lombok.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;

@Repository
public interface PersonRepository extends PagingAndSortingRepository<Person, Long> {

    @Query("SELECT p FROM Person p " +
            "WHERE p.gender = :gender " +
            "AND (p.motheredFamilies IS EMPTY OR (SIZE(p.motheredFamilies) = 1)) " +
            "AND p.fatheredFamilies IS EMPTY " +
            "AND p.birthDate < :aliveOnDate " +
            "AND p.deathDate > :aliveOnDate " +
            "AND (:minAgeAtDeath IS NULL OR (YEAR(p.deathDate) - YEAR(p.birthDate)) >= :minAgeAtDeath) " +
            "AND (CAST(:minBirthDate AS date) IS NULL OR p.birthDate >= :minBirthDate) " +
            "AND (CAST(:maxBirthDate AS date) IS NULL OR p.birthDate <= :maxBirthDate) " +
            "ORDER BY p.birthDate")
    List<Person> findPotentialSpouses(@Param("gender") @NonNull Gender gender,
                                      @Param("aliveOnDate") @NonNull LocalDate aliveOnDate,
                                      @Param("minBirthDate") @Nullable LocalDate minBirthDate,
                                      @Param("maxBirthDate") @Nullable LocalDate maxBirthDate,
                                      @Param("minAgeAtDeath") @NonNull Integer minAgeAtDeath);


}
