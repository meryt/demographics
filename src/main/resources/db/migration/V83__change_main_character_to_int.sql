ALTER TABLE persons DROP COLUMN main_character;
ALTER TABLE persons ADD COLUMN main_character INTEGER DEFAULT NULL;
COMMENT ON COLUMN persons.main_character IS 'Non-null values indicate a main character. Use this as an index to allow sort by importance of character (low values first)';
