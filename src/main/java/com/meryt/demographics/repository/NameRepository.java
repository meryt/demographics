package com.meryt.demographics.repository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.database.QueryStore;
import com.meryt.demographics.domain.person.Gender;

@Repository
public class NameRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final QueryStore queryStore = new QueryStore("name");

    public NameRepository(@Autowired NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @NonNull
    public String randomFirstName(@NonNull Gender gender, @Nullable Set<String> excludeNames, @Nullable LocalDate onDate) {
        String name;
        do {
            String query = queryStore.getQuery("random-first-name");
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("gender", gender.getAbbreviation());
            params.addValue("onDate", onDate);
            Map<String, Object> result = jdbcTemplate.queryForMap(query, params);
            name = (String) result.get("name");
        } while (excludeNames != null && excludeNames.contains(name));
        return name;
    }

    @NonNull
    public String randomLastName() {
        String query = "SELECT name FROM names_last ORDER BY random() LIMIT 1";
        return jdbcTemplate.queryForObject(query, Collections.emptyMap(), String.class);
    }
}
