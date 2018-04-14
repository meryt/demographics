package com.meryt.demographics.repository;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AncestryRepository  {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AncestryRepository(@Autowired NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Truncates and rebuilds the ancestry table in the database.
     */
    public void updateAncestryTable() {
        jdbcTemplate.queryForRowSet("SELECT * FROM rebuild_ancestry()", Collections.emptyMap());
    }
}
