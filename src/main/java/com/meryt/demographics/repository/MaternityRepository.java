package com.meryt.demographics.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.person.fertility.Maternity;

@Repository
public interface MaternityRepository extends CrudRepository<Maternity, Long> {
}
