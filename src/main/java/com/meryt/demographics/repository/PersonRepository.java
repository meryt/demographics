package com.meryt.demographics.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.person.Person;

@Repository
public interface PersonRepository extends PagingAndSortingRepository<Person, Long> {

}
