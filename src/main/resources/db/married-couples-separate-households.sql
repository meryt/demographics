SELECT 
    p.id, p.first_name, p.last_name,
    p2.id, p2.first_name, p2.last_name,
    hi.household_id, hi2.household_id
FROM living_persons p
INNER JOIN families f ON p.id = f.husband_id
INNER JOIN living_persons p2 ON f.wife_id = p2.id
LEFT JOIN household_inhabitants hi ON p.id = hi.person_id AND DATERANGE(hi.from_date, hi.to_date) @> (SELECT last_check_date FROM check_date)
LEFT JOIN household_inhabitants hi2 on p2.id = hi2.person_id AND DATERANGE(hi2.from_date, hi2.to_date) @> (SELECT last_check_date FROM check_date)
WHERE hi.household_id != hi2.household_id

