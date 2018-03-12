
-- Point the household at the households table instead
ALTER TABLE household_inhabitants DROP CONSTRAINT household_inhabitants_household_id_fkey;

ALTER TABLE household_inhabitants ADD FOREIGN KEY (household_id) REFERENCES households (id) ON DELETE NO ACTION;


-- Fix the household head constraint
ALTER TABLE household_inhabitants DROP CONSTRAINT unique_household_head_per_daterange;

ALTER TABLE household_inhabitants
ADD CONSTRAINT unique_household_head_per_daterange
EXCLUDE USING gist (household_id WITH =, daterange(from_date, to_date, '[)') WITH &&) WHERE (is_household_head);
