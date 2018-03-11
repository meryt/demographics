
ALTER TABLE occupations RENAME COLUMN min_class TO min_class_int;
ALTER TABLE occupations RENAME COLUMN max_class TO max_class_int;

ALTER TABLE occupations ADD COLUMN min_class TEXT;
ALTER TABLE occupations ADD COLUMN max_class TEXT;

UPDATE occupations SET min_class = 'PAUPER' WHERE min_class_int = 1;
UPDATE occupations SET min_class = 'LABORER' WHERE min_class_int = 2;
UPDATE occupations SET min_class = 'LANDOWNER_OR_CRAFTSMAN' WHERE min_class_int = 3;
UPDATE occupations SET min_class = 'YEOMAN_OR_MERCHANT' WHERE min_class_int = 4;
UPDATE occupations SET min_class = 'GENTLEMAN' WHERE min_class_int = 5;
UPDATE occupations SET min_class = 'BARONET' WHERE min_class_int = 6;

UPDATE occupations SET max_class = 'PAUPER' WHERE max_class_int = 1;
UPDATE occupations SET max_class = 'LABORER' WHERE max_class_int = 2;
UPDATE occupations SET max_class = 'LANDOWNER_OR_CRAFTSMAN' WHERE max_class_int = 3;
UPDATE occupations SET max_class = 'YEOMAN_OR_MERCHANT' WHERE max_class_int = 4;
UPDATE occupations SET max_class = 'GENTLEMAN' WHERE max_class_int = 5;
UPDATE occupations SET max_class = 'BARONET' WHERE max_class_int = 6;

ALTER TABLE occupations DROP COLUMN min_class_int;
ALTER TABLE occupations DROP COLUMN max_class_int;
