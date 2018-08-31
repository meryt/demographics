CREATE TABLE configuration (
  key   TEXT NOT NULL PRIMARY KEY,
  value TEXT
);

INSERT INTO configuration VALUES ('stop_check', false);
