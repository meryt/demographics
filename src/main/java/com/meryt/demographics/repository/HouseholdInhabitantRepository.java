package com.meryt.demographics.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.place.HouseholdInhabitantPK;
import com.meryt.demographics.domain.place.HouseholdInhabitantPeriod;

@Repository
public interface HouseholdInhabitantRepository extends CrudRepository<HouseholdInhabitantPeriod, HouseholdInhabitantPK> {

}
