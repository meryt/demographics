
-- Use the Irish names over a longer period until I can find better data for earlier dates.
-- Otherwise people born before 1800 were all getting early medieval Irish names.
UPDATE names_first set from_date = '1600-01-01' where culture = 'IRELAND' and from_date = '1800-01-01';
