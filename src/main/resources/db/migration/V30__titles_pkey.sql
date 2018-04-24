ALTER TABLE person_titles DROP CONSTRAINT person_titles_pkey;

ALTER TABLE person_titles ADD PRIMARY KEY(person_id, title_id, from_date);
