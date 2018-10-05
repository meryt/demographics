ALTER TABLE dwelling_place_owners DROP CONSTRAINT unique_owner_period_per_dwelling_place_per_daterange;
ALTER TABLE dwelling_place_owners ADD CONSTRAINT unique_owner_per_dwelling_place_per_daterange
EXCLUDE USING gist (dwelling_place_id WITH =, daterange(from_date, to_date, '[)'::text) WITH &&)
  DEFERRABLE INITIALLY DEFERRED;
