CREATE TABLE dwelling_places (
    id                  SERIAL PRIMARY KEY,
    parent_id           INTEGER,
    dwelling_place_type TEXT NOT NULL,
    name                TEXT,
    acres               FLOAT,
    FOREIGN KEY (parent_id) REFERENCES dwelling_places (id) ON DELETE SET NULL
);

CREATE INDEX idx_dwelling_places_parent_id ON dwelling_places (parent_id);
CREATE INDEX idx_dwelling_places_type ON dwelling_places (dwelling_place_type);

CREATE TABLE dwelling_place_parents (
    dwelling_place_id  INTEGER NOT NULL,
    parent_id          INTEGER,
    from_date          DATE NOT NULL,
    to_date            DATE,
    PRIMARY KEY (dwelling_place_id, from_date),
    FOREIGN KEY (dwelling_place_id) REFERENCES dwelling_places (id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES dwelling_places (id) ON DELETE SET NULL
);

CREATE INDEX idx_dwelling_places_parents_parent_id ON dwelling_place_parents (parent_id);

ALTER TABLE dwelling_place_parents
ADD CONSTRAINT unique_parent_per_place_per_daterange
EXCLUDE USING gist (dwelling_place_id WITH =, daterange(from_date, to_date, '[)') WITH &&);

CREATE TABLE household_inhabitants (
    household_id       INTEGER NOT NULL,
    person_id          INTEGER NOT NULL,
    from_date          DATE NOT NULL,
    to_date            DATE,
    is_household_head  BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (person_id, from_date),
    FOREIGN KEY (person_id) REFERENCES persons (id) ON DELETE CASCADE,
    FOREIGN KEY (household_id) REFERENCES dwelling_places (id)
);

ALTER TABLE household_inhabitants
ADD CONSTRAINT unique_household_per_person_per_daterange
EXCLUDE USING gist (person_id WITH =, daterange(from_date, to_date, '[)') WITH &&);

ALTER TABLE household_inhabitants
ADD CONSTRAINT unique_household_head_per_daterange
EXCLUDE USING gist (household_id WITH =, daterange(from_date, to_date, '[)') WITH &&);
