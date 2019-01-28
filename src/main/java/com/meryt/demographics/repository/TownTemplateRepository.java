package com.meryt.demographics.repository;

import java.util.Collections;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TownTemplateRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TownTemplateRepository(@Autowired NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @NonNull
    public String getUnusedMapId(boolean isEngland) {
        String regionCondition = isEngland ? "valid_england" : "valid_scotland";
        String query = String.format(
                "SELECT tt.town_map_id FROM town_templates tt " +
                        "LEFT JOIN dwelling_places dp ON dp.map_id = tt.town_map_id AND dp.dwelling_place_type = 'TOWN' " +
                        "WHERE %s IS TRUE AND dp.id IS NULL " +
                        "ORDER BY random() LIMIT 1",
                regionCondition);
        return jdbcTemplate.queryForObject(query, Collections.emptyMap(), String.class);
    }

}
