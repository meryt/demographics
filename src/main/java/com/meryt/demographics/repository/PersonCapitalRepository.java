package com.meryt.demographics.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.person.PersonCapitalPK;
import com.meryt.demographics.domain.person.PersonCapitalPeriod;

@Repository
public interface PersonCapitalRepository extends CrudRepository<PersonCapitalPeriod, PersonCapitalPK> {
}
