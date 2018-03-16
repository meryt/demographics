
ALTER TABLE occupations ADD COLUMN is_rural BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE occupations SET is_rural = TRUE WHERE NAME LIKE 'Farmer%';
UPDATE occupations SET is_rural = TRUE WHERE NAME LIKE 'Cowkeeper%';
UPDATE occupations SET is_rural = TRUE WHERE NAME LIKE 'Agricultural%';
