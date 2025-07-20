package com.meryt.demographics.repository;

import java.time.LocalDate;
import java.util.List;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.domain.person.SocialClass;

@Repository
public interface PersonRepository extends PagingAndSortingRepository<Person, Long>, CrudRepository<Person, Long> {

    List<Person> findByDeathDate(LocalDate deathDate);

    List<Person> findByFounderTrueOrderByBirthDate();

    List<Person> findByStoryCharacterIsTrue();

    @Query("SELECT p from Person p " +
            "WHERE p.birthDate < :aliveOnDate " +
            "AND p.deathDate > :aliveOnDate " +
            "ORDER BY p.birthDate ")
    List<Person> findAllLiving(@Param("aliveOnDate") @NonNull LocalDate onDate);

    @Query("SELECT p from Person p " +
            "WHERE p.gender = :gender " +
            "AND p.birthDate < :aliveOnDate " +
            "AND p.deathDate > :aliveOnDate " +
            "AND p.socialClass IN (:socialClasses) " +
            "ORDER BY p.birthDate ")
    List<Person> findBySocialClassAndGenderAndIsLiving(@Param("socialClasses") @NonNull List<SocialClass> socialClasses,
                                                       @Param("gender") @NonNull Gender gender,
                                                       @Param("aliveOnDate") @NonNull LocalDate onDate);

    @Query("SELECT p FROM Person p " +
            "WHERE (:gender IS NULL OR p.gender = :gender) " +
            "AND p.motheredFamilies IS EMPTY " +
            "AND p.fatheredFamilies IS EMPTY " +
            "AND p.socialClass IN (:socialClasses) " +
            "AND (CAST(:minBirthDate AS date) IS NULL OR p.birthDate >= :minBirthDate) " +
            "AND (CAST(:maxBirthDate AS date) IS NULL OR p.birthDate <= :maxBirthDate) " +
            "AND p.deathDate > :aliveOnDate " +
            "ORDER BY p.birthDate"
    )
    List<Person> findUnmarriedPeopleBySocialClassAndGenderAndAge(@Param("socialClasses")
                                                                              @NonNull List<SocialClass> socialClasses,
                                                                 @Param("gender")
                                                                              @Nullable Gender gender,
                                                                 @Param("minBirthDate")
                                                                              @Nullable LocalDate minBirthDate,
                                                                 @Param("maxBirthDate")
                                                                              @Nullable LocalDate maxBirthDate,
                                                                 @Param("aliveOnDate")
                                                                              @NonNull LocalDate aliveOnDate);


    @Query("SELECT p FROM Person p " +
                    "WHERE p.birthDate <= :aliveOnDate " +
                    "AND p.deathDate > :aliveOnDate " +
                    "AND p.storyCharacter = TRUE " +
                    "ORDER BY p.birthDate "
    )
    List<Person> findLivingStoryCharacters(@Param("aliveOnDate") @NonNull LocalDate onDate);

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
            "AND p.storyCharacter = FALSE " +
            "ORDER BY p.birthDate")
    List<Person> findPotentialSpouses(@Param("gender") @NonNull Gender gender,
                                      @Param("aliveOnDate") @NonNull LocalDate aliveOnDate,
                                      @Param("minBirthDate") @Nullable LocalDate minBirthDate,
                                      @Param("maxBirthDate") @Nullable LocalDate maxBirthDate,
                                      @Param("minAgeAtDeath") @NonNull Integer minAgeAtDeath);

    @Query("SELECT p FROM Person p " +
            "WHERE (:gender IS NULL OR p.gender = :gender) " +
            "AND (p.fatheredFamilies IS EMPTY OR NOT EXISTS " +
            "    (SELECT f " +
            "     FROM Family f " +
            "     JOIN Person p2 ON f.wife = p2 " +
            "     WHERE f.husband = p AND p2.deathDate >= :aliveOnDate)) " +
            "AND (p.motheredFamilies IS EMPTY OR NOT EXISTS " +
            "    (SELECT f " +
            "     FROM Family f " +
            "     JOIN Person p2 ON f.husband = p2 " +
            "     WHERE f.wife = p AND p2.deathDate >= :aliveOnDate)) " +
            "AND p.birthDate < :aliveOnDate " +
            "AND p.deathDate > :aliveOnDate " +
            "AND (CAST(:minBirthDate AS date) IS NULL OR p.birthDate >= :minBirthDate) " +
            "AND (CAST(:maxBirthDate AS date) IS NULL OR p.birthDate <= :maxBirthDate) " +
            "AND p.finishedGeneration = FALSE " +
            "AND p.storyCharacter = FALSE " +
            "ORDER BY p.birthDate")
    List<Person> findUnmarriedPeople(@Param("aliveOnDate") @NonNull LocalDate aliveOnDate,
                                     @Param("minBirthDate") @NonNull LocalDate minBirthDate,
                                     @Param("maxBirthDate") @NonNull LocalDate maxBirthDate,
                                     @Param("gender") @Nullable Gender gender);

    @Query("SELECT p FROM Person p " +
            "WHERE (:gender IS NULL OR p.gender = :gender) " +
            "AND p.finishedGeneration = FALSE " +
            "ORDER BY p.birthDate")
    List<Person> findUnfinishedPersons(@Param("gender") @Nullable Gender gender);

    @Query("SELECT p FROM Person p " +
            "WHERE (:gender IS NULL OR p.gender = :gender) " +
            "AND p.finishedGeneration = FALSE " +
            "AND p.households IS EMPTY " +
            "ORDER BY p.birthDate")
    List<Person> findUnfinishedNonResidents(@Param("gender") @Nullable Gender gender);

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
            "AND (p.maternity.conceptionDate IS NOT NULL OR (p.maternity.father IS NOT NULL AND p.maternity.havingRelations = TRUE)) " +
            "AND YEAR(CAST(:check_date AS date)) - YEAR(p.birthDate) >= 13 " +
            "AND YEAR(CAST(:check_date AS date)) - YEAR(p.birthDate) <= 55 " +
            "ORDER BY p.birthDate")
    List<Person> findWomenWithPendingMaternities(@NonNull @Param("check_date") LocalDate checkDate);

}
