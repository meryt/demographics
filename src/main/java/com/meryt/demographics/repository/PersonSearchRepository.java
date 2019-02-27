package com.meryt.demographics.repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.repository.criteria.PersonCriteria;

@Repository
public class PersonSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public Page<Person> findPersons(@NonNull PersonCriteria personCriteria) {
        String query = "SELECT * FROM persons p ";

        PersonCriteria.JoinsAndConditions joinsAndConditions = personCriteria.getJoinsAndConditions();
        query += joinsAndConditions.getJoins().stream().collect(Collectors.joining("\n"));
        query += " WHERE TRUE AND " + joinsAndConditions.getWhereClause();
        query += joinsAndConditions.getOrderBy();

        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(y.*) AS cnt FROM (" + query + ") y");
        Query nativeQuery = entityManager.createNativeQuery(query + personCriteria.getLimitAndOffset(), Person.class);

        for (Map.Entry<String, Object> params : joinsAndConditions.getParameters().entrySet()) {
            countQuery.setParameter(params.getKey(), params.getValue());
            nativeQuery.setParameter(params.getKey(), params.getValue());
        }

        List<Person> persons = (List<Person>) nativeQuery.getResultList();
        BigInteger countResults = (BigInteger) countQuery.getSingleResult();

        return new PageImpl<>(persons, personCriteria.getPageRequest(), countResults.intValue());
    }

}
