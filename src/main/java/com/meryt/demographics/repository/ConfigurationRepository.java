package com.meryt.demographics.repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.ConfigurationConstants;

@Repository
public class ConfigurationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ConfigurationRepository(@Autowired NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isPauseCheck() {
        String value = getValue(ConfigurationConstants.CONFIG_STOP_CHECK);
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    public void pauseCheck() {
        setValue(ConfigurationConstants.CONFIG_STOP_CHECK, Boolean.valueOf(true).toString());
    }

    public void unpauseCheck() {
        setValue(ConfigurationConstants.CONFIG_STOP_CHECK, Boolean.valueOf(false).toString());
    }

    public void setValue(@NonNull String key, @Nullable String value) {
        String query = "UPDATE configuration SET value = :value WHERE key = :key";
        Map<String, String> params = new HashMap<>();
        params.put("key", key);
        params.put("value", value);
        jdbcTemplate.update(query, params);
    }

    public Map<String, String> getAllConfiguration() {
        String query = "SELECT key, value::TEXT FROM configuration " +
                       "UNION " +
                       "SELECT 'last_check_date' AS key, last_check_date::TEXT AS value FROM check_date";
        return jdbcTemplate.query(query, (rs, rowNum) -> {
            Map<String, String> row = new HashMap<>();
            row.put("key", rs.getString("key"));
            row.put("value", rs.getString("value"));
            return row;
        }).stream().collect(Collectors.toMap(
            row -> row.get("key"),
            row -> row.get("value")
        ));
    }

    @Nullable
    private String getValue(@NonNull String key) {
        String query = "SELECT value::TEXT FROM configuration WHERE key = :key";
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(query, Collections.singletonMap("key", key));
            return (String) result.get("value");
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
