-- This query deletes all persons, places, etc. but does not touch static data like name or occupation lists

BEGIN;

DELETE FROM families;
DELETE FROM dwelling_places;
DELETE FROM household_inhabitants;
DELETE FROM households;
DELETE FROM persons;

select 'COMMIT now if you wish to delete all of the persons, places, etc.' as message;

