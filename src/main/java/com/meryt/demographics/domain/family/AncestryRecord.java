package com.meryt.demographics.domain.family;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AncestryRecord {
    private long ancestorId;
    private long descendantId;
    private String via;
    private String path;
    private long distance;
}
