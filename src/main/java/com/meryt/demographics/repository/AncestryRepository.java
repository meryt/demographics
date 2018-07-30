package com.meryt.demographics.repository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.meryt.demographics.domain.family.AncestryRecord;
import com.meryt.demographics.domain.family.LeastCommonAncestorRelationship;
import com.meryt.demographics.repository.rowmappers.LeastCommonAncestorRelationshipMapper;

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
            return Collections.emptyList();
        }
    }

    /**
     * Gets all persons related to this person, living or dead
     * @param personId the person (will be subject1 in the results
     * @return 0 or more relationship records (the record relating a person to himself is not included)
     */
    @NonNull
    public List<LeastCommonAncestorRelationship> getRelatives(long personId, @Nullable Long maxDistance) {
        String query =
                "WITH rels AS (" +
                        "SELECT DISTINCT ON (subject_2) *, " +
                        "(subject_1_distance + subject_2_distance) AS distance " +
                        "FROM least_common_ancestors " +
                        "WHERE subject_1 = :personId " +
                        "AND subject_2 != :personId " +
                        "AND (:maxDistance IS NULL OR (subject_1_distance + subject_2_distance <= :maxDistance)) " +
                        "ORDER BY subject_2, (subject_1_distance + subject_2_distance)) " +
                        "SELECT * FROM rels ORDER BY distance; ";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("personId", personId);
        params.addValue("maxDistance", maxDistance);
        try {
            return jdbcTemplate.query(query, params, new LeastCommonAncestorRelationshipMapper());
        } catch (EmptyResultDataAccessException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Gets all persons related to this person, living on the given date
     * @param personId the person (will be subject1 in the results
     * @param onDate the date used to determine whether the person is living
     * @return 0 or more relationship records (the record relating a person to himself is not included)
     */
    @NonNull
    public List<LeastCommonAncestorRelationship> getLivingRelatives(long personId,
                                                                    @NonNull LocalDate onDate,
                                                                    @Nullable Long maxDistance) {
        String query =
                "WITH relas AS (" +
                        "SELECT DISTINCT ON (lca.subject_2) lca.*, " +
                        "(lca.subject_1_distance + lca.subject_2_distance) AS distance " +
                        "FROM least_common_ancestors lca " +
                        "INNER JOIN persons p ON lca.subject_2 = p.id " +
                        "WHERE lca.subject_1 = :personId " +
                        "AND lca.subject_2 != :personId " +
                        "AND p.birth_date <= CAST(:onDate AS DATE) " +
                        "AND p.death_date > CAST(:onDate AS DATE) " +
                        "AND (:maxDistance IS NULL OR (lca.subject_1_distance + lca.subject_2_distance <= :maxDistance)) " +
                        "ORDER BY lca.subject_2, (lca.subject_1_distance + lca.subject_2_distance)) " +
                        "SELECT * FROM relas ORDER BY distance ";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("personId", personId);
        params.addValue("onDate", onDate);
        params.addValue("maxDistance", maxDistance);
        try {
            return jdbcTemplate.query(query, params, new LeastCommonAncestorRelationshipMapper());
        } catch (EmptyResultDataAccessException e) {
            return Collections.emptyList();
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
            return jdbcTemplate.queryForObject(query, params, new LeastCommonAncestorRelationshipMapper());
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
