
ALTER TABLE dwelling_places ADD COLUMN entailed_title_id INT;

ALTER TABLE dwelling_places ADD FOREIGN KEY (entailed_title_id) REFERENCES titles (id) ON DELETE SET NULL;
