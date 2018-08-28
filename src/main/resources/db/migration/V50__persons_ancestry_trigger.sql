CREATE OR REPLACE FUNCTION persons_ancestry_tr_func()
  RETURNS trigger
LANGUAGE plpgsql
AS $function$
DECLARE
  father_id INTEGER;
  mother_id INTEGER;
BEGIN
  IF TG_OP = 'UPDATE' AND OLD.family_id IS NOT DISTINCT FROM NEW.family_id THEN
    RETURN NEW;
  END IF;

  DELETE FROM ancestry WHERE descendant_id = NEW.id;

  INSERT INTO ancestry VALUES (NEW.id, NEW.id, NULL, NULL, 0);

  father_id := (SELECT f.husband_id FROM families f WHERE f.id = NEW.family_id);
  mother_id := (SELECT f.wife_id FROM families f WHERE f.id = NEW.family_id);

  INSERT INTO ancestry
    SELECT
      a.ancestor_id,
      NEW.id,
      CASE WHEN a.distance = 0 THEN NULL WHEN a.distance = 1 THEN a.descendant_id::TEXT ELSE a.via || ',' || a.descendant_id END AS via,
      CASE WHEN a.distance = 0 THEN a.ancestor_id || ',' || NEW.id ELSE a.path || ',' || NEW.id END AS path,
      distance + 1 AS distance
    FROM ancestry a WHERE a.descendant_id = father_id OR a.descendant_id = mother_id
  ;

  RETURN NEW;

END;
$function$
;

DROP TRIGGER IF EXISTS persons_ancestry_tr ON persons;

CREATE TRIGGER persons_ancestry_tr
  AFTER INSERT OR UPDATE ON persons
  FOR EACH ROW EXECUTE PROCEDURE persons_ancestry_tr_func();
