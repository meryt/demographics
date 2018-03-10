package com.meryt.demographics.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.family.Family;

@Repository
public interface FamilyRepository extends CrudRepository<Family, Long> {
}
