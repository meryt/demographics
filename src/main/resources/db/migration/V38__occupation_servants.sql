ALTER TABLE occupations ADD COLUMN is_domestic_servant BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE occupations ADD COLUMN is_farm_laborer BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE occupations SET is_domestic_servant = TRUE WHERE name IN (
  'Domestic servant',
  'Gardener',
  'Messenger, porter and errand boy'
);

UPDATE occupations SET is_farm_laborer = TRUE WHERE name IN (
  'Agricultural labourer, farm servant, shepherd'
);
