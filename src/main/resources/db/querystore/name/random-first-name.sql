WITH y AS (
    SELECT
      name,
      weight,
      SUM(weight) OVER (ORDER BY name) AS cum_weight
    FROM names_first
    WHERE gender = :gender
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
