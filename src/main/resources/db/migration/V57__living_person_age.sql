CREATE OR REPLACE VIEW living_persons AS
  SELECT *,
    DATE_PART('year', age((SELECT last_check_date FROM check_date), birth_date)) AS age
  FROM persons
  WHERE birth_date <= (SELECT last_check_date FROM check_date)
        AND death_date > (SELECT last_check_date FROM check_date);
