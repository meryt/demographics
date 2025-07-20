ALTER TABLE household_inhabitants DROP CONSTRAINT IF EXISTS household_inhabitants_household_id_fkey;

ALTER TABLE household_inhabitants ADD CONSTRAINT household_inhabitants_household_id_fkey 
    FOREIGN KEY (household_id) REFERENCES households (id) ON DELETE CASCADE;
