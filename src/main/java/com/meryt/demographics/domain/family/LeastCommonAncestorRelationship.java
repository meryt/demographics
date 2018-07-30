package com.meryt.demographics.domain.family;

import lombok.Data;

@Data
public class LeastCommonAncestorRelationship {
    private long subject1;
    private long subject2;
    private long leastCommonAncestor;
    private String subject1Via;
    private int subject1Distance;
    private String subject2Via;
    private int subject2Distance;

    public int getDistance() {
        return subject1Distance + subject2Distance;
    }
}
