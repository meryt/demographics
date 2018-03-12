
DROP TABLE dwelling_place_parents;

CREATE TABLE households (
  id                  SERIAL PRIMARY KEY,
  parent_id           INTEGER,
  name                TEXT,
  FOREIGN KEY (parent_id) REFERENCES dwelling_places (id) ON DELETE SET NULL
);

