package com.meryt.demographics.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


import com.meryt.demographics.domain.place.Household;

@Repository
public interface HouseholdRepository extends CrudRepository<Household, Long> {

}
