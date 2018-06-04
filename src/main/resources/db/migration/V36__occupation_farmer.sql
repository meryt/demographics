
ALTER TABLE occupations ADD COLUMN is_farm_owner BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE occupations SET is_farm_owner = TRUE WHERE NAME LIKE 'Farmer%';
UPDATE occupations SET is_farm_owner = TRUE WHERE NAME LIKE 'Cowkeeper%';
