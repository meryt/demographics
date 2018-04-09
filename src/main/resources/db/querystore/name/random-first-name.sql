/* WITH z AS (
  SELECT
    CASE WHEN (:onDate::DATE IS NOT NULL AND EXISTS (
        SELECT from_date FROM names_first WHERE gender = :gender AND DATERANGE(from_date, to_date, '[)') @> :onDate::DATE)) THEN :onDate::DATE
      WHEN :onDate::DATE IS NOT NULL AND (SELECT MIN(from_date) FROM names_first WHERE gender = :gender) > :onDate::DATE)
        THEN (SELECT MIN(from_date) FROM names_first WHERE gender = :gender)
      ELSE                                          ELSE
        SELECT MIN(from_date) FROM names_first WHERE gender = :gender AND from_date <
    MIN(from_date)
  FROM names_first WHERE gender = :gender
  AND (:onDate::DATE IS NOT NULL AND DATERANGE(from_date, to_date, '[)') @> :onDate::DATE)
), */
WITH
  y AS (
    SELECT
      name,
      weight,
      SUM(weight) OVER (ORDER BY name) AS cum_weight
    FROM names_first
    WHERE gender = :gender
    AND (:onDate::DATE IS NULL OR DATERANGE(from_date, to_date, '[)') @> :onDate::DATE )
    ORDER BY name
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
