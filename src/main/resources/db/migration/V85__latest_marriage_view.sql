
-- For people who are/were married, shows the death
-- date of their last spouse. This shows the earliest
-- date at which they could remarry.
CREATE VIEW person_last_marriage AS
SELECT
    p.id,
    MAX(sp.death_date) AS last_married_date
FROM persons p
JOIN families f
  ON p.id IN (f.husband_id, f.wife_id)
JOIN persons sp
  ON sp.id =
     CASE
       WHEN p.id = f.husband_id THEN f.wife_id
       ELSE f.husband_id
     END
GROUP BY p.id;
