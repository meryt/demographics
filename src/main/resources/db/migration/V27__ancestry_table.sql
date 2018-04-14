
CREATE TABLE ancestry (
    ancestor_id   INTEGER NOT NULL,
    descendant_id INTEGER NOT NULL,
    via           TEXT,
    path          TEXT,
    distance      INTEGER,
    PRIMARY KEY (ancestor_id, descendant_id),
    FOREIGN KEY (ancestor_id) REFERENCES persons(id) ON DELETE CASCADE,
    FOREIGN KEY (descendant_id) REFERENCES persons(id) ON DELETE CASCADE
);

CREATE TABLE least_common_ancestors (
    subject_1             INTEGER NOT NULL,
    subject_2             INTEGER NOT NULL,
    least_common_ancestor INTEGER NOT NULL,
    subject_1_via         TEXT,
    subject_1_distance    INTEGER,
    subject_2_via         TEXT,
    subject_2_distance    INTEGER,
    PRIMARY KEY (subject_1, subject_2)
);
