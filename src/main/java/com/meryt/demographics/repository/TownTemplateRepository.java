package com.meryt.demographics.repository;

import java.util.Collections;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
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

    @Nullable
    public Pair<String, Double> getClosestAvailablePolygonForMapId(@NonNull String mapId, double desiredValue) {
        Pair<String, Double> result = getAvailablePolygonForMapIdBelowValue(mapId, desiredValue);
        if (result != null) {
            return result;
        }
        return getAvailablePolygonForMapIdAboveValue(mapId, desiredValue);
    }

    private Pair<String, Double> getAvailablePolygonForMapIdBelowValue(@NonNull String mapId, double desiredValue) {

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("value", desiredValue);
        params.addValue("mapId", mapId);

        String query = "SELECT tht.polygon_id, tht.polygon_value " +
                "FROM town_house_templates tht " +
                "LEFT JOIN dwelling_places town ON tht.town_map_id = town.map_id " +
                "LEFT JOIN dwelling_places dp ON tht.polygon_id = dp.map_id " +
                "    AND dp.dwelling_place_type = 'DWELLING' " +
                "    AND dp.parent_id = town.id " +
                "WHERE dp.id IS NULL " +
                "AND tht.polygon_value <= :value " +
                "AND tht.town_map_id = :mapId " +
                "ORDER BY tht.polygon_value DESC LIMIT 1";

        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(query, params);
            return Pair.of((String) result.get("polygon_id"), new Double((Integer) result.get("polygon_value")));
        } catch (DataAccessException e) {
            return null;
        }
    }

    private Pair<String, Double> getAvailablePolygonForMapIdAboveValue(@NonNull String mapId, double desiredValue) {

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("value", desiredValue);
        params.addValue("mapId", mapId);

        String query = "SELECT tht.polygon_id, tht.polygon_value " +
                "FROM town_house_templates tht " +
                "LEFT JOIN dwelling_places town ON tht.town_map_id = town.map_id " +
                "LEFT JOIN dwelling_places dp ON tht.polygon_id = dp.map_id " +
                "    AND dp.dwelling_place_type = 'DWELLING' " +
                "    AND dp.parent_id = town.id " +
                "WHERE dp.id IS NULL " +
                "AND tht.polygon_value >= :value " +
                "AND tht.town_map_id = :mapId " +
                "ORDER BY tht.polygon_value ASC LIMIT 1";

        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(query, params);
            return Pair.of((String) result.get("polygon_id"), new Double(((Integer) result.get("polygon_value"))));
        } catch (DataAccessException e) {
            return null;
        }
    }
}
