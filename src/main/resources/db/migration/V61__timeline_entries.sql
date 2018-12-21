
CREATE TABLE timeline_entries (
  id               SERIAL PRIMARY KEY,
  content          TEXT NOT NULL,
  title            TEXT,
  category         TEXT NOT NULL,
  from_date        DATE NOT NULL,
  to_date          DATE
);

CREATE TABLE person_timeline_entries (
  timeline_entry_id  INTEGER NOT NULL,
  person_id          INTEGER NOT NULL,
  PRIMARY KEY (timeline_entry_id, person_id),
  FOREIGN KEY (timeline_entry_id) REFERENCES timeline_entries (id),
  FOREIGN KEY (person_id) REFERENCES persons (id)
);


