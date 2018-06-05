
CREATE TABLE dwelling_place_owners (
  dwelling_place_id   INTEGER NOT NULL,
  person_id           INTEGER NOT NULL,
  from_date           DATE NOT NULL,
  to_date             DATE,
  PRIMARY KEY (dwelling_place_id, from_date),
  FOREIGN KEY (dwelling_place_id) REFERENCES dwelling_places (id) ON DELETE CASCADE,
  FOREIGN KEY (person_id) REFERENCES persons (id) ON DELETE CASCADE
);

CREATE INDEX idx_dwelling_place_owners_person ON dwelling_place_owners (person_id);

ALTER TABLE dwelling_place_owners
  ADD CONSTRAINT unique_owner_per_dwelling_place_per_daterange
EXCLUDE USING gist (dwelling_place_id WITH =, daterange(from_date, to_date, '[)') WITH &&);
