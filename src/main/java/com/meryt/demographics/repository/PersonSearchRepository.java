package com.meryt.demographics.repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.repository.criteria.PersonCriteria;

@Repository
@Slf4j
public class PersonSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public Page<Person> findPersons(@NonNull PersonCriteria personCriteria) {
        String query = "SELECT p.* FROM persons p ";

        PersonCriteria.JoinsAndConditions joinsAndConditions = personCriteria.getJoinsAndConditions();
        query += joinsAndConditions.getJoins().stream().collect(Collectors.joining("\n"));
        if (!joinsAndConditions.getWhereClauses().isEmpty()) {
            query += " WHERE TRUE AND " + joinsAndConditions.getWhereClauses().stream().collect(Collectors.joining(" AND "));
        }
        query += joinsAndConditions.getOrderBy();

        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(y.*) AS cnt FROM (" + query + ") y");
        Query nativeQuery = entityManager.createNativeQuery(query + personCriteria.getLimitAndOffset(), Person.class);
        log.info("Query: {}", query);
        log.info("Parameters: {}", joinsAndConditions.getParameters());

        for (Map.Entry<String, Object> params : joinsAndConditions.getParameters().entrySet()) {
            countQuery.setParameter(params.getKey(), params.getValue());
            nativeQuery.setParameter(params.getKey(), params.getValue());
        }

        List<Person> persons = (List<Person>) nativeQuery.getResultList();
        Long countResults = (Long) countQuery.getSingleResult();

        return new PageImpl<>(persons, personCriteria.getPageRequest(), countResults.intValue());
    }

}
