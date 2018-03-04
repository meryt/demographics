package com.meryt.demographics.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.meryt.demographics.database.QueryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LifeTableRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final QueryStore queryStore = new QueryStore("life-table");

    public LifeTableRepository(@Autowired NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Get the lx values for the given time period (e.g. "victorian" era).
     *
     * The lx values are an array of doubles such that the index is the age and the value is the percent likelihood
     * that a person will live to at least that age. The value of getLxvalues()[0] is 1.0 and decreases from there.
     *
     * @param period the period from the life table (e.g. "victorian" or "medieval"
     * @return an array of 0-indexed lx values such that the index 0 is age 0 and so forth.
     */
    public double[] getLxValues(String period) {
        String query =
                "SELECT "
                + "total_living::numeric / (SELECT total_living FROM life_table WHERE age = 0 AND period = :period) AS lx "
                + "FROM life_table "
                + "WHERE period = :period "
                + "ORDER BY age";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("period", period);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(query, params);
        double[] lxValues = new double[results.size()];
        for (int i = 0; i < results.size(); i++) {
            lxValues[i] = ((BigDecimal) results.get(i).get("lx")).doubleValue();
        }
        return lxValues;
    }

}