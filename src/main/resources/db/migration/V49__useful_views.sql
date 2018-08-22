CREATE OR REPLACE VIEW living_persons AS
SELECT * FROM persons
WHERE birth_date <= (SELECT last_check_date FROM check_date)
  AND death_date > (SELECT last_check_date FROM check_date);

CREATE OR REPLACE VIEW richest_persons AS
SELECT
  pc.person_id AS id,
  p.first_name,
  p.last_name,
  ROUND(pc.capital) AS capital,
  hi.household_id AS household,
  hdp.dwelling_place_id AS dwelling
FROM person_capital pc
INNER JOIN persons p
    ON pc.person_id = p.id
LEFT JOIN household_inhabitants hi
    ON p.id = hi.person_id
    AND daterange(hi.from_date, hi.to_date) @> (SELECT last_check_date FROM check_date)
LEFT JOIN household_locations hdp
    ON hi.household_id = hdp.household_id
    AND daterange(hdp.from_date, hdp.to_date) @> (SELECT last_check_date FROM check_date)
WHERE daterange(pc.from_date, pc.to_date) @> (SELECT last_check_date FROM check_date)
ORDER BY capital DESC;
