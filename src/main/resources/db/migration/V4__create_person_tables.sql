CREATE TABLE persons (
    id           SERIAL PRIMARY KEY,
    family_id    INTEGER,
    first_name   TEXT,
    middle_names TEXT,
    last_name    TEXT,
    gender       TEXT NOT NULL,
    birth_date   DATE,
    birth_place  TEXT,
    death_date   DATE,
    death_place  TEXT,
    social_class TEXT,
    domesticity  FLOAT,
    charisma     FLOAT,
    comeliness   FLOAT,
    intelligence FLOAT,
    morality     FLOAT,
    strength     FLOAT
);

COMMENT ON COLUMN persons.family_id is 'The ID of the family the person is born into (i.e. of which he is a child).';

CREATE TABLE families (
    id            SERIAL PRIMARY KEY,
    husband_id    INTEGER,
    wife_id       INTEGER,
    wedding_date  DATE,
    wedding_place TEXT
);

ALTER TABLE persons
   ADD CONSTRAINT fk_person_family
   FOREIGN KEY (family_id)
   REFERENCES families(id);

ALTER TABLE families
    ADD CONSTRAINT fk_family_husband
    FOREIGN KEY (husband_id)
    REFERENCES persons(id);

ALTER TABLE families
    ADD CONSTRAINT fk_family_wife
    FOREIGN KEY (wife_id)
    REFERENCES persons(id);
