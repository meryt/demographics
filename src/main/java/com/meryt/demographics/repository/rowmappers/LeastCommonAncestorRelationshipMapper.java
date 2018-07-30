package com.meryt.demographics.repository.rowmappers;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.annotation.Nullable;
import org.springframework.jdbc.core.RowMapper;

import com.meryt.demographics.domain.family.LeastCommonAncestorRelationship;

public class LeastCommonAncestorRelationshipMapper implements RowMapper<LeastCommonAncestorRelationship> {

    @Override
    @Nullable
    public LeastCommonAncestorRelationship mapRow(ResultSet rs, int rowNum) throws SQLException {
        LeastCommonAncestorRelationship rel = new LeastCommonAncestorRelationship();
        rel.setSubject1(rs.getLong("subject_1"));
        rel.setSubject2(rs.getLong("subject_2"));
        rel.setLeastCommonAncestor(rs.getLong("least_common_ancestor"));
        rel.setSubject1Via(rs.getString("subject_1_via"));
        rel.setSubject2Via(rs.getString("subject_2_via"));
        rel.setSubject1Distance(rs.getInt("subject_1_distance"));
        rel.setSubject2Distance(rs.getInt("subject_2_distance"));
        return rel;
    }
}
