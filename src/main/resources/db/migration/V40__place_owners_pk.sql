
ALTER TABLE dwelling_place_owners DROP CONSTRAINT dwelling_place_owners_pkey;
ALTER TABLE dwelling_place_owners DROP CONSTRAINT unique_owner_per_dwelling_place_per_daterange;

ALTER TABLE dwelling_place_owners ADD PRIMARY KEY (dwelling_place_id, person_id, from_date);

ALTER TABLE dwelling_place_owners
  ADD CONSTRAINT unique_owner_period_per_dwelling_place_per_daterange
EXCLUDE USING gist (dwelling_place_id WITH =, person_id WITH =, daterange(from_date, to_date, '[)') WITH &&);
