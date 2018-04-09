WITH
min_from_date AS (
   SELECT MIN(from_date) AS from_date
   FROM names_first
   WHERE gender = :gender
),
next_lowest_from_date AS (
    SELECT MAX(from_date) AS from_date
    FROM names_first
    WHERE gender = :gender
    AND :onDate::DATE IS NOT NULL AND from_date < :onDate::DATE
),
containing_from_date AS (
     SELECT :onDate::DATE AS from_date
     FROM names_first
     WHERE gender = :gender
     AND :onDate::DATE IS NOT NULL AND DATERANGE(from_date, to_date, '[)') @> :onDate::DATE
     LIMIT 1
),
y AS (
    SELECT
      nf.name,
      nf.weight,
      SUM(nf.weight) OVER (ORDER BY nf.name) AS cum_weight
    FROM names_first nf, min_from_date, next_lowest_from_date, containing_from_date
    WHERE nf.gender = :gender
    AND (DATERANGE(nf.from_date, nf.to_date, '[)') @> COALESCE(containing_from_date.from_date, next_lowest_from_date.from_date, min_from_date.from_date))
    ORDER BY nf.name
),
rand AS (
      SELECT
        SUM(weight) AS total_weight,
        random()*(SELECT SUM(weight) FROM y) AS randval
      FROM y
  )
SELECT
  y.name,
  y.weight,
  rand.total_weight,
  rand.randval
FROM y
  CROSS JOIN rand
WHERE y.cum_weight - y.weight <= rand.randval
ORDER BY cum_weight DESC
LIMIT 1;
