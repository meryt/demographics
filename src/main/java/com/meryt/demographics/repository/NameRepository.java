package com.meryt.demographics.repository;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.database.QueryStore;
import com.meryt.demographics.domain.person.FirstName;
import com.meryt.demographics.domain.person.Gender;
import com.meryt.demographics.domain.person.LastName;
import com.meryt.demographics.rest.BadRequestException;

@Repository
public class NameRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final QueryStore queryStore = new QueryStore("name");

    public NameRepository(@Autowired NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @NonNull
    public FirstName randomFirstNameObject(@NonNull Gender gender, @Nullable Set<String> excludeNames, @Nullable LocalDate onDate, @Nullable Set<String> cultures) {
        FirstName firstName;
        do {
            String query = queryStore.getQuery("random-first-name");
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("gender", gender.getAbbreviation());
            params.addValue("onDate", onDate);
            addCulturesParameter(params, cultures);
            
            try {
                firstName = jdbcTemplate.queryForObject(query, params, (rs, rowNum) -> {
                    FirstName fn = new FirstName();
                    fn.setName(rs.getString("name"));
                    fn.setWeight(rs.getDouble("weight"));
                    fn.setGender(Gender.from(rs.getString("gender")));
                    int rank = rs.getInt("rank");
                    if (!rs.wasNull()) {
                        fn.setRank(rank);
                    }
                    fn.setCulture(rs.getString("culture"));
                    java.sql.Date fromDate = rs.getDate("from_date");
                    if (fromDate != null) {
                        fn.setFromDate(fromDate.toLocalDate());
                    }
                    java.sql.Date toDate = rs.getDate("to_date");
                    if (toDate != null) {
                        fn.setToDate(toDate.toLocalDate());
                    }
                    return fn;
                });
            } catch (EmptyResultDataAccessException e) {
                throw new BadRequestException("No first name found for gender " + gender + " and cultures " + cultures);
            }
        } while (excludeNames != null && excludeNames.contains(firstName.getName()));
        return firstName;
    }

    @NonNull
    public String randomFirstName(@NonNull Gender gender, @Nullable Set<String> excludeNames, @Nullable LocalDate onDate, @Nullable Set<String> cultures) {
        return randomFirstNameObject(gender, excludeNames, onDate, cultures).getName();
    }

    @NonNull
    public LastName randomLastNameObject(@Nullable Set<String> cultures) {
        String query;
        MapSqlParameterSource params = new MapSqlParameterSource();
        
        if (cultures == null || cultures.isEmpty()) {
            query = "SELECT name, culture FROM names_last ORDER BY random() LIMIT 1";
        } else {
            query = "SELECT name, culture FROM names_last WHERE culture = ANY(:cultures::TEXT[]) ORDER BY random() LIMIT 1";
            addCulturesParameter(params, cultures);
        }
        
        try {
            return jdbcTemplate.queryForObject(query, params, (rs, rowNum) -> {
                LastName lastName = new LastName();
                lastName.setName(rs.getString("name"));
                lastName.setCulture(rs.getString("culture"));
                return lastName;
            });
        } catch (EmptyResultDataAccessException e) {
            throw new BadRequestException("No last name found for cultures " + cultures);
        }
    }

    @NonNull
    public String randomLastName(@Nullable Set<String> cultures) {
        return randomLastNameObject(cultures).getName();
    }

    /**
     * Converts a Set of culture strings to a PostgreSQL array and adds it to the parameter source.
     *
     * @param params the parameter source to add the cultures parameter to
     * @param cultures the set of cultures to convert, may be null or empty
     */
    private void addCulturesParameter(@NonNull MapSqlParameterSource params, @Nullable Set<String> cultures) {
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
    }

}
