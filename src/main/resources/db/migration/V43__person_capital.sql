CREATE TABLE person_capital (
  person_id      INTEGER NOT NULL,
  capital        DOUBLE PRECISION NOT NULL,
  from_date      DATE NOT NULL,
  to_date        DATE,
  PRIMARY KEY (person_id, from_date),
  FOREIGN KEY (person_id) REFERENCES persons (id) ON DELETE CASCADE
);

ALTER TABLE person_capital
  ADD CONSTRAINT unique_capital_per_person_per_daterange
EXCLUDE USING gist (person_id WITH =, daterange(from_date, to_date, '[)') WITH &&);
