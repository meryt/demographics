WITH incomes AS (
SELECT
    pc.person_id,
    DATERANGE(pc.from_date, pc.to_date) AS capital_period,
    pc.capital,
    pc.capital - lag(pc.capital) OVER (partition by pc.person_id order by pc.from_date) AS income
FROM person_capital pc
INNER JOIN living_persons lp ON pc.person_id = lp.id
WHERE DATERANGE(pc.from_date, pc.to_date) && (SELECT DATERANGE(CAST(last_check_date - INTERVAL '5 years' AS DATE), last_check_date) FROM check_date)
ORDER BY pc.person_id, pc.from_date
), avg_incomes_ungrouped AS (
SELECT
    i.person_id,
    AVG(i.capital) OVER (PARTITION BY i.person_id) AS avg_capital,
    AVG(i.income) OVER (PARTITION BY i.person_id) AS avg_income
FROM incomes i
WHERE i.income IS NOT NULL
), avg_incomes AS (
SELECT
    person_id,
    ROUND(avg_capital) AS avg_capital,
    ROUND(avg_income) AS avg_income,
    CASE WHEN ROUND(avg_capital) = 0 OR ROUND(avg_income) <= 0 THEN 0 ELSE ROUND((avg_income / avg_capital)::NUMERIC * 100, 2) END AS income_percent_of_capital,
    ROUND(0.04 * avg_capital) AS expected_interest
FROM avg_incomes_ungrouped
GROUP BY 1, 2, 3, 4, 5
ORDER BY 4 DESC
), household_incomes AS (
SELECT
    hi.household_id,
    SUM(i.avg_capital) AS total_avg_capital,
    SUM(i.avg_income) AS total_avg_income,
    ROUND(AVG(i.income_percent_of_capital)::NUMERIC, 2) AS avg_income_percent_capital,
    CASE
        WHEN ROUND(AVG(i.income_percent_of_capital)::NUMERIC, 2) BETWEEN 10 AND 40
            THEN ROUND(SUM(i.avg_income)::NUMERIC, 2)
        ELSE ROUND(0.03 * SUM(i.avg_capital))
    END AS expected_income
FROM household_inhabitants hi
INNER JOIN living_persons lp
    ON hi.person_id = lp.id
INNER JOIN household_locations hl
    ON hi.household_id = hl.household_id
    AND DATERANGE(hl.from_date, hl.to_date) @> (SELECT last_check_date FROM check_date)
LEFT JOIN avg_incomes i
    ON lp.id = i.person_id
WHERE DATERANGE(hi.from_date, hi.to_date) @> (SELECT last_check_date FROM check_date)
GROUP BY hi.household_id
)
SELECT
    hi.household_id,
    hi.person_id,
    CASE WHEN hi.is_household_head THEN 'Y' ELSE null END AS head,
    lp.first_name,
    lp.last_name,
    hinc.total_avg_capital,
    hinc.total_avg_income,
    hinc.avg_income_percent_capital AS income_percent,
    ROUND(0.03 * hinc.total_avg_capital) AS expected_interest,
    hinc.expected_income,
    lp.social_class AS actual_class,
    CASE
        WHEN expected_income > 10000 THEN 'BARONET'
        WHEN expected_income > 2000 THEN 'GENTLEMAN'
        WHEN expected_income > 1000 THEN 'YEOMAN_OR_MERCHANT'
        WHEN expected_income > 100 THEN 'LANDOWNER_OR_CRAFTSMAN'
        WHEN expected_income > 30 THEN 'LABORER'
        ELSE 'PAUPER'
    END AS expected_class
FROM household_inhabitants hi
INNER JOIN living_persons lp
    ON hi.person_id = lp.id
INNER JOIN social_classes sc
    ON lp.social_class = sc.id
INNER JOIN household_locations hl
    ON hi.household_id = hl.household_id
    AND DATERANGE(hl.from_date, hl.to_date) @> (SELECT last_check_date FROM check_date)
LEFT JOIN household_incomes hinc
    ON hi.household_id = hinc.household_id
WHERE DATERANGE(hi.from_date, hi.to_date) @> (SELECT last_check_date FROM check_date)
AND CASE
        WHEN expected_income > 10000 THEN 'BARONET'
        WHEN expected_income > 2000 THEN 'GENTLEMAN'
        WHEN expected_income > 1000 THEN 'YEOMAN_OR_MERCHANT'
        WHEN expected_income > 100 THEN 'LANDOWNER_OR_CRAFTSMAN'
        WHEN expected_income > 30 THEN 'LABORER'
        ELSE 'PAUPER'
    END != lp.social_class
AND sc.rank >= 5
ORDER BY household_id, head NULLS LAST

/*
y AS (
SELECT
    hi.household_id,
    hi.person_id,
    CASE WHEN hi.is_household_head THEN 'Y' ELSE null END AS head,
    lp.first_name,
    lp.last_name,
    lp.social_class,
    i.avg_capital,
    i.avg_income,
    i.income_percent_of_capital,
    i.expected_interest
FROM household_inhabitants hi
INNER JOIN living_persons lp
    ON hi.person_id = lp.id
INNER JOIN household_locations hl
    ON hi.household_id = hl.household_id
    AND DATERANGE(hl.from_date, hl.to_date) @> (SELECT last_check_date FROM check_date)
LEFT JOIN avg_incomes i
    ON lp.id = i.person_id
WHERE DATERANGE(hi.from_date, hi.to_date) @> (SELECT last_check_date FROM check_date)
ORDER BY household_id, head NULLS LAST
)
SELECT * FROM y


SELECT
    y.social_class,
    sc.rank,
    COUNT(y.social_class)
FROM y
INNER JOIN social_classes sc ON y.social_class = sc.id
GROUP BY 1, 2
ORDER BY 2
*/