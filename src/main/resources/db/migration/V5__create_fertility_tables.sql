CREATE TABLE paternities (
    person_id          INTEGER PRIMARY KEY,
    fertility_factor   FLOAT
);

CREATE TABLE maternities (
    person_id          INTEGER PRIMARY KEY,
    fertility_factor   FLOAT,
    frequency_factor   FLOAT,
    withdrawal_factor  FLOAT,
    father_id          INTEGER,
    conception_date    DATE,
    miscarriage_date   DATE,
    due_date           DATE,
    last_cycle_date    DATE,
    last_check_date    DATE,
    last_birth_date    DATE,
    breastfeeding_till DATE,
    had_twins          BOOLEAN NOT NULL DEFAULT FALSE,
    having_relations   BOOLEAN NOT NULL DEFAULT TRUE,
    fraternal_twins    BOOLEAN NOT NULL DEFAULT FALSE,
    identical_twins    BOOLEAN NOT NULL DEFAULT FALSE,
    num_births         SMALLINT NOT NULL DEFAULT 0,
    num_miscarriages   SMALLINT NOT NULL DEFAULT 0,
    cycle_length       SMALLINT NOT NULL DEFAULT 28
);

CREATE INDEX idx_maternities_father_id ON maternities (father_id);

ALTER TABLE paternities
   ADD CONSTRAINT fk_paternity_person
   FOREIGN KEY (person_id)
   REFERENCES persons(id)
   ON DELETE CASCADE;

ALTER TABLE maternities
   ADD CONSTRAINT fk_maternity_person
   FOREIGN KEY (person_id)
   REFERENCES persons(id)
   ON DELETE CASCADE;

ALTER TABLE maternities
    ADD CONSTRAINT fk_maternity_father
    FOREIGN KEY (father_id)
    REFERENCES persons(id)
    ON DELETE SET NULL;

CREATE INDEX idx_families_husband_id ON families (husband_id);
CREATE INDEX idx_families_wife_id ON families (wife_id);
CREATE INDEX idx_persons_family_id ON persons (family_id);
