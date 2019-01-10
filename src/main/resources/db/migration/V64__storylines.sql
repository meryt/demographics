CREATE TABLE storylines (
  id     SERIAL PRIMARY KEY,
  name   TEXT
);

CREATE TABLE storyline_timeline_entries (
  storyline_id      INTEGER NOT NULL,
  timeline_entry_id INTEGER NOT NULL,
  PRIMARY KEY (storyline_id, timeline_entry_id),
  FOREIGN KEY (storyline_id) REFERENCES storylines (id) ON DELETE CASCADE,
  FOREIGN KEY (timeline_entry_id) REFERENCES timeline_entries (id) ON DELETE CASCADE
);

CREATE TABLE person_storylines (
  person_id    INTEGER NOT NULL,
  storyline_id INTEGER NOT NULL,
  PRIMARY KEY (person_id, storyline_id),
  FOREIGN KEY (person_id) REFERENCES persons (id) ON DELETE CASCADE,
  FOREIGN KEY (storyline_id) REFERENCES storylines (id) ON DELETE CASCADE
);

