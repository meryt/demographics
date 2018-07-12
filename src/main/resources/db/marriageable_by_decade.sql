SELECT
  d.decade + 20 as marriage_year,
  COUNT(p.id)
FROM (SELECT t * 10 AS decade FROM generate_series(100, 190) AS t) AS d
LEFT JOIN persons p
    ON FLOOR(EXTRACT(YEAR FROM p.birth_date) / 10) * 10 = d.decade
WHERE p.finished_generation IS FALSE
GROUP BY marriage_year
ORDER BY marriage_year
