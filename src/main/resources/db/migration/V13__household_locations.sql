
CREATE TABLE household_locations (
  household_id       INTEGER NOT NULL,
  dwelling_place_id  INTEGER NOT NULL,
  from_date          DATE NOT NULL,
  to_date            DATE,
  PRIMARY KEY (household_id, from_date),
  FOREIGN KEY (dwelling_place_id) REFERENCES dwelling_places (id) ON DELETE CASCADE,
  FOREIGN KEY (household_id) REFERENCES households (id) ON DELETE CASCADE
);

CREATE INDEX idx_household_locations_place ON household_locations (dwelling_place_id);

ALTER TABLE household_locations
  ADD CONSTRAINT unique_location_per_household_per_daterange
EXCLUDE USING gist (household_id WITH =, daterange(from_date, to_date, '[)') WITH &&);
