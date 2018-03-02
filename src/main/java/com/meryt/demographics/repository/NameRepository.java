package com.meryt.demographics.repository;

import java.util.Collections;
import java.util.Map;
import com.meryt.demographics.database.QueryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.person.Gender;

@Repository
public class NameRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final QueryStore queryStore = new QueryStore("name");

    public NameRepository(@Autowired NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String randomFirstName(Gender gender) {
        String query = queryStore.getQuery("random-first-name");
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("gender", gender.getAbbreviation());
        Map<String, Object> result = jdbcTemplate.queryForMap(query, params);
        return (String) result.get("name");
    }

    public String randomLastName() {
        String query = "SELECT name FROM names_last ORDER BY random() LIMIT 1";
        return jdbcTemplate.queryForObject(query, Collections.emptyMap(), String.class);
    }
}
