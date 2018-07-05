package com.meryt.demographics.repository;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.family.AncestryRecord;
import com.meryt.demographics.domain.family.LeastCommonAncestorRelationship;

@Repository
public class AncestryRepository  {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AncestryRepository(@Autowired NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @NonNull
    public List<AncestryRecord> getDescendants(long personId) {
        String query = "SELECT ancestor_id, descendant_id, via, path, distance FROM ancestry " +
                "WHERE ancestor_id = :ancestor " +
                "AND ancestor_id != descendant_id " +
                "ORDER BY distance, path";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ancestor", personId);
        try {
            return jdbcTemplate.query(query, params, (rs, rowNum) -> {
                AncestryRecord rec = new AncestryRecord();
                rec.setAncestorId(rs.getLong("ancestor_id"));
                rec.setDescendantId(rs.getLong("descendant_id"));
                rec.setVia(rs.getString("via"));
                rec.setPath(rs.getString("path"));
                rec.setDistance(rs.getInt("distance"));
                return rec;
            });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Gets a record representing a least common ancestor relationship between two people.
     * @param person1Id the first person
     * @param person2Id the second person
     * @return a LeastCommonAncestorRelationship or null if they are not blood relatives
     */
    @Nullable
    public LeastCommonAncestorRelationship getLeastCommonAncestorInfo(long person1Id, long person2Id) {
        String query = "SELECT " +
                                "subject_1, " +
                                "subject_2, " +
                                "least_common_ancestor, " +
                                "subject_1_via, " +
                                "subject_1_distance, " +
                                "subject_2_via, " +
                                "subject_2_distance " +
                        "FROM least_common_ancestors " +
                        "WHERE subject_1 = :person1Id AND subject_2 = :person2Id " +
                        "ORDER BY (subject_1_distance + subject_2_distance), subject_1_distance, subject_2_distance " +
                        "LIMIT 1";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("person1Id", person1Id);
        params.addValue("person2Id", person2Id);

        try {
            return jdbcTemplate.queryForObject(query, params, (rs, rowNum) -> {
                LeastCommonAncestorRelationship rel = new LeastCommonAncestorRelationship();
                rel.setSubject1(rs.getLong("subject_1"));
                rel.setSubject2(rs.getLong("subject_2"));
                rel.setLeastCommonAncestor(rs.getLong("least_common_ancestor"));
                rel.setSubject1Via(rs.getString("subject_1_via"));
                rel.setSubject2Via(rs.getString("subject_2_via"));
                rel.setSubject1Distance(rs.getInt("subject_1_distance"));
                rel.setSubject2Distance(rs.getInt("subject_2_distance"));
                return rel;
            });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * Truncates and rebuilds the ancestry table in the database.
     */
    public void updateAncestryTable() {
        jdbcTemplate.queryForRowSet("SELECT * FROM rebuild_ancestry()", Collections.emptyMap());
    }
}
