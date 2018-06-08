package com.meryt.demographics.repository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * This repository manages the check_date table, which keeps track of the last date for which auto-generation has
 * been run.
 */
@Repository
public class CheckDateRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CheckDateRepository(@Autowired NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Nullable
    public LocalDate getCurrentDate() {
        String query = "SELECT last_check_date::TEXT FROM check_date";
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(query, Collections.emptyMap());
            String date = (String) result.get("last_check_date");
            return LocalDate.parse(date);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public void setCurrentDate(@NonNull LocalDate date) {
        if (getCurrentDate() == null) {
            insertCurrentDate(date);
        } else {
            updateCurrentDate(date);
        }
    }

    private void insertCurrentDate(@NonNull LocalDate date) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("new_date", date);

        String query = "INSERT INTO check_date (last_check_date) VALUES (:new_date)";
        jdbcTemplate.update(query, params);
    }

    private void updateCurrentDate(@NonNull LocalDate date) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("new_date", date);

        String query = "UPDATE check_date SET last_check_date = :new_date";
        jdbcTemplate.update(query, params);
    }

}
