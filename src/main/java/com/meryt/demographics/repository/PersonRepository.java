package com.meryt.demographics.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.person.Person;

@Repository
public interface PersonRepository extends CrudRepository<Person, Long> {

}
