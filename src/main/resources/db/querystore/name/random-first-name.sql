WITH
min_from_date AS (
   SELECT 3 as rank, MIN(from_date) AS from_date
   FROM names_first
   WHERE gender = :gender
   AND (:cultures::TEXT[] IS NULL OR culture = ANY(:cultures::TEXT[]))
),
next_lowest_from_date AS (
    SELECT 2 as rank, MAX(from_date) AS from_date
    FROM names_first
    WHERE gender = :gender
    AND (:cultures::TEXT[] IS NULL OR culture = ANY(:cultures::TEXT[]))
    AND :onDate::DATE IS NOT NULL AND from_date < :onDate::DATE
),
containing_from_date AS (
     SELECT 1 AS rank, :onDate::DATE AS from_date
     FROM names_first
     WHERE gender = :gender
     AND (:cultures::TEXT[] IS NULL OR culture = ANY(:cultures::TEXT[]))
     AND :onDate::DATE IS NOT NULL AND DATERANGE(from_date, to_date, '[)') @> :onDate::DATE
     LIMIT 1
),
best_date AS (
    SELECT z.from_date FROM
      (
        SELECT * FROM containing_from_date
        UNION
        SELECT * FROM next_lowest_from_date
        UNION
        SELECT * FROM min_from_date
      ) z WHERE z.from_date IS NOT NULL ORDER BY z.rank
    LIMIT 1
),
y AS (
    SELECT
      nf.name,
      nf.weight,
      SUM(nf.weight) OVER (ORDER BY nf.name, nf.from_date) AS cum_weight
    FROM names_first nf, best_date
    WHERE nf.gender = :gender
    AND (:cultures::TEXT[] IS NULL OR nf.culture = ANY(:cultures::TEXT[]))
    AND (DATERANGE(nf.from_date, nf.to_date, '[)') @> best_date.from_date)
    ORDER BY nf.name, nf.from_date
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
