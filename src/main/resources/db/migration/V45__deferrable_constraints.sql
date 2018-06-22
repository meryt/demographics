ALTER TABLE person_capital DROP CONSTRAINT unique_capital_per_person_per_daterange;
ALTER TABLE person_capital ADD CONSTRAINT unique_capital_per_person_per_daterange
  EXCLUDE USING gist (person_id WITH =, daterange(from_date, to_date, '[)'::TEXT) WITH &&)
  DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE household_locations DROP CONSTRAINT unique_location_per_household_per_daterange;
ALTER TABLE household_locations ADD CONSTRAINT unique_location_per_household_per_daterange
  EXCLUDE USING gist (household_id WITH =, daterange(from_date, to_date, '[)'::text) WITH &&)
  DEFERRABLE INITIALLY DEFERRED;


ALTER TABLE household_inhabitants DROP CONSTRAINT unique_household_head_per_daterange;
ALTER TABLE household_inhabitants ADD CONSTRAINT unique_household_head_per_daterange
  EXCLUDE USING gist (household_id WITH =, daterange(from_date, to_date, '[)'::text) WITH &&)
  WHERE (is_household_head)
  DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE household_inhabitants DROP CONSTRAINT unique_household_per_person_per_daterange;
ALTER TABLE household_inhabitants ADD CONSTRAINT unique_household_per_person_per_daterange
  EXCLUDE USING gist (person_id WITH =, daterange(from_date, to_date, '[)'::text) WITH &&)
  DEFERRABLE INITIALLY DEFERRED;




