package com.meryt.demographics.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.person.Trait;

@Repository
public class TraitRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TraitRepository(@Autowired NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Gets :num random distinct traits
     *
     * @param num the number of traits to return
     * @return a list of Traits with no duplicates
     */
    @NonNull
    public List<Trait> randomTraits(int num) {
        if (num <= 0) {
            return new ArrayList<>();
        }
        String query = "SELECT id, rating, name FROM traits ORDER BY random() LIMIT " + num;
        return jdbcTemplate.query(query, Collections.emptyMap(), new BeanPropertyRowMapper<>(Trait.class));
    }

}
