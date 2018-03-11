
ALTER TABLE occupations ADD PRIMARY KEY (id);

CREATE TABLE person_occupations (
    person_id      INTEGER NOT NULL,
    occupation_id  INTEGER NOT NULL,
    from_date      DATE NOT NULL,
    to_date        DATE,
    PRIMARY KEY (person_id, from_date),
    FOREIGN KEY (person_id) REFERENCES persons (id) ON DELETE CASCADE,
    FOREIGN KEY (occupation_id) REFERENCES occupations (id) ON DELETE CASCADE
);

-- Must be superuser
-- CREATE EXTENSION btree_gist;

ALTER TABLE person_occupations
ADD CONSTRAINT unique_occupation_per_person_per_daterange
EXCLUDE USING gist (person_id WITH =, daterange(from_date, to_date, '[)') WITH &&);
