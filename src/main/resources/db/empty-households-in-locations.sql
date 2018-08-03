BEGIN;
WITH empty_households AS (
    SELECT
      hl.household_id,
      hl.dwelling_place_id,
      hl.from_date,
      hl.to_date,
      COUNT(hi.person_id) AS num_inhabitants
    FROM household_locations hl
      LEFT JOIN household_inhabitants hi
        ON hl.household_id = hi.household_id
           AND DATERANGE(hi.from_date, hi.to_date) @> (SELECT last_check_date
                                                       from check_date
                                                       limit 1)
    WHERE DATERANGE(hl.from_date, hl.to_date) @> (SELECT last_check_date
                                                  from check_date
                                                  limit 1)
    GROUP BY 1, 2, 3, 4
    HAVING COUNT(hi.person_id) = 0
),
empty_to_dates AS (
      SELECT
        eh.household_id,
        eh.dwelling_place_id,
        eh.from_date,
        MAX(hi.to_date) AS last_resident_to_date
      FROM empty_households eh
        LEFT JOIN household_inhabitants hi
          ON eh.household_id = hi.household_id
      GROUP BY 1, 2, 3
)

UPDATE household_locations hl
SET to_date = etd.last_resident_to_date
FROM empty_to_dates etd
WHERE hl.household_id = etd.household_id
      AND hl.dwelling_place_id = etd.dwelling_place_id
AND hl.to_date IS NULL;

--select * From empty_to_dates
