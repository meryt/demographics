SELECT
  hi.household_id,
  COUNT(hi.person_id)
FROM household_inhabitants hi
WHERE DATERANGE(hi.from_date, hi.to_date) @> '1460-01-01'::DATE
GROUP BY hi.household_id
HAVING COUNT(hi.person_id) > 8;
