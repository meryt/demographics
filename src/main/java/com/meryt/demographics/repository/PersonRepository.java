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

    List<Person> findByFounderTrueOrderByBirthDate();

    @Query("SELECT p FROM Person p " +
            "WHERE p.gender = :gender " +
            "AND (p.motheredFamilies IS EMPTY OR (SIZE(p.motheredFamilies) = 1)) " +
            "AND p.fatheredFamilies IS EMPTY " +
            "AND p.birthDate < :aliveOnDate " +
            "AND p.deathDate > :aliveOnDate " +
            "AND (:minAgeAtDeath IS NULL OR (YEAR(p.deathDate) - YEAR(p.birthDate)) >= :minAgeAtDeath) " +
            "AND (CAST(:minBirthDate AS date) IS NULL OR p.birthDate >= :minBirthDate) " +
            "AND (CAST(:maxBirthDate AS date) IS NULL OR p.birthDate <= :maxBirthDate) " +
            "AND p.finishedGeneration = FALSE " +
            "ORDER BY p.birthDate")
    List<Person> findPotentialSpouses(@Param("gender") @NonNull Gender gender,
                                      @Param("aliveOnDate") @NonNull LocalDate aliveOnDate,
                                      @Param("minBirthDate") @Nullable LocalDate minBirthDate,
                                      @Param("maxBirthDate") @Nullable LocalDate maxBirthDate,
                                      @Param("minAgeAtDeath") @NonNull Integer minAgeAtDeath);

    @Query("SELECT p FROM Person p " +
            "WHERE p.gender = 'MALE' " +
            "AND (p.fatheredFamilies IS EMPTY OR NOT EXISTS " +
            "    (SELECT f " +
            "     FROM Family f " +
            "     JOIN Person p2 ON f.wife = p2 " +
            "     WHERE f.husband = p AND p2.deathDate >= :aliveOnDate)) " +
            "AND p.birthDate < :aliveOnDate " +
            "AND p.deathDate > :aliveOnDate " +
            "AND (CAST(:minBirthDate AS date) IS NULL OR p.birthDate >= :minBirthDate) " +
            "AND (CAST(:maxBirthDate AS date) IS NULL OR p.birthDate <= :maxBirthDate) " +
            "AND p.finishedGeneration = FALSE " +
            "ORDER BY p.birthDate")
    List<Person> findUnmarriedMen(@Param("aliveOnDate") @NonNull LocalDate aliveOnDate,
                                  @Param("minBirthDate") @NonNull LocalDate minBirthDate,
                                  @Param("maxBirthDate") @NonNull LocalDate maxBirthDate);

    @Query("SELECT p FROM Person p " +
            "WHERE (:gender IS NULL OR p.gender = :gender) " +
            "AND p.finishedGeneration = FALSE " +
            "ORDER BY p.birthDate")
    List<Person> findUnfinishedPersons(@Param("gender") @Nullable Gender gender);

    /**
     * Gets all women who are living on or before this date and whose last check day is on or before this date
     * @param checkDate the date the check should be done. Will find women whose last check day was well before this
     *                  date, not only 1 day behind
     * @return a list of women, possibly empty
     */
    @NonNull
    @Query("SELECT p from Person p " +
            "WHERE p.gender = 'FEMALE' " +
            "AND p.maternity.lastCheckDate < :check_date " +
            "AND p.deathDate > p.maternity.lastCheckDate " +
            "ORDER BY p.birthDate")
    List<Person> findWomenWithPendingMaternities(@NonNull @Param("check_date") LocalDate checkDate);
}
