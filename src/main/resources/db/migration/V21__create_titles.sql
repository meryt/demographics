

CREATE TABLE titles (
  id               SERIAL PRIMARY KEY,
  name             TEXT,
  social_class     TEXT NOT NULL,
  peerage          TEXT NOT NULL DEFAULT 'ENGLAND',
  inheritance      TEXT NOT NULL DEFAULT 'HEIRS_MALE_OF_THE_BODY',
  inheritance_root INTEGER,
  FOREIGN KEY (social_class) REFERENCES social_classes (id),
  FOREIGN KEY (inheritance_root) REFERENCES persons (id) ON DELETE SET NULL
);

CREATE INDEX idx_title_social_class ON titles (social_class);
CREATE INDEX idx_title_inheritance_root ON titles (inheritance_root);

--INSERT INTO titles (name, social_class, peerage, inheritance) VALUES
--  ('Lord Rowe of Breckanburn', 'BARON', 'SCOTLAND', 'HEIRS_OF_THE_BODY');

CREATE TABLE person_titles (
  person_id     INTEGER NOT NULL,
  title_id      INTEGER NOT NULL,
  from_date     DATE NOT NULL,
  to_date       DATE,
  PRIMARY KEY (person_id, from_date),
  FOREIGN KEY (person_id) REFERENCES persons (id) ON DELETE CASCADE,
  FOREIGN KEY (title_id) REFERENCES titles (id) ON DELETE CASCADE
);

CREATE INDEX idx_person_title_title_id ON person_titles(title_id);

-- Ensure the same title can't be held by two people simultaneously
ALTER TABLE person_titles
  ADD CONSTRAINT unique_title_per_daterange
EXCLUDE USING gist (title_id WITH =, daterange(from_date, to_date, '[)') WITH &&);
