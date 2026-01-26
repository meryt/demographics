package com.meryt.demographics.repository;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
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
    public String randomFirstName(@NonNull Gender gender, @Nullable Set<String> excludeNames, @Nullable LocalDate onDate, @Nullable Set<String> cultures) {
        String name;
        do {
            String query = queryStore.getQuery("random-first-name");
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("gender", gender.getAbbreviation());
            params.addValue("onDate", onDate);
            
            // Convert Set to PostgreSQL array
            if (cultures != null && !cultures.isEmpty()) {
                Connection conn = DataSourceUtils.getConnection(jdbcTemplate.getJdbcTemplate().getDataSource());
                try {
                    Array array = conn.createArrayOf("TEXT", cultures.toArray());
                    params.addValue("cultures", array);
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to create PostgreSQL array for cultures", e);
                } finally {
                    DataSourceUtils.releaseConnection(conn, jdbcTemplate.getJdbcTemplate().getDataSource());
                }
            } else {
                params.addValue("cultures", null);
            }
            
            Map<String, Object> result = jdbcTemplate.queryForMap(query, params);
            name = (String) result.get("name");
        } while (excludeNames != null && excludeNames.contains(name));
        return name;
    }

    @NonNull
    public String randomLastName(@Nullable Set<String> cultures) {
        String query;
        MapSqlParameterSource params = new MapSqlParameterSource();
        
        if (cultures == null || cultures.isEmpty()) {
            query = "SELECT name FROM names_last ORDER BY random() LIMIT 1";
        } else {
            query = "SELECT name FROM names_last WHERE culture = ANY(:cultures::TEXT[]) ORDER BY random() LIMIT 1";
            
            // Convert Set to PostgreSQL array
            Connection conn = DataSourceUtils.getConnection(jdbcTemplate.getJdbcTemplate().getDataSource());
            try {
                Array array = conn.createArrayOf("TEXT", cultures.toArray());
                params.addValue("cultures", array);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create PostgreSQL array for cultures", e);
            } finally {
                DataSourceUtils.releaseConnection(conn, jdbcTemplate.getJdbcTemplate().getDataSource());
            }
        }
        
        return jdbcTemplate.queryForObject(query, params, String.class);
    }
}
