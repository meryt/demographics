
ALTER TABLE persons ADD COLUMN finished_generation BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE persons ADD COLUMN is_founder BOOLEAN NOT NULL DEFAULT FALSE;
