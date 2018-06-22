-- This query deletes all persons, places, etc. but does not touch static data like name or occupation lists

BEGIN;

DELETE FROM families;
DELETE FROM dwelling_places;
DELETE FROM household_inhabitants;
DELETE FROM households;
DELETE FROM persons;
DELETE FROM titles;
DELETE FROM check_date;

ALTER SEQUENCE families_id_seq RESTART;
ALTER SEQUENCE dwelling_places_id_seq RESTART;
ALTER SEQUENCE households_id_seq RESTART;
ALTER SEQUENCE persons_id_seq RESTART;
ALTER SEQUENCE titles_id_seq RESTART;

select 'COMMIT now if you wish to delete all of the persons, places, etc.' as message;

