

CREATE OR REPLACE FUNCTION rebuild_ancestry() RETURNS VOID AS $$

BEGIN

TRUNCATE TABLE ancestry;

INSERT INTO ancestry (
    SELECT
        f.husband_id AS ancestor_id,
        p.id AS descendant_id,
        NULL AS via,
        f.husband_id || ',' || p.id AS path,
        1 AS distance
    FROM families f INNER JOIN persons p
        ON f.id = p.family_id
    WHERE f.husband_id IS NOT NULL
);

INSERT INTO ancestry (
    SELECT
        f.wife_id AS ancestor_id,
        p.id AS descendant_id,
        NULL AS via,
        f.wife_id || ',' || p.id AS path,
        1 AS distance
    FROM families f INNER JOIN persons p
        ON f.id = p.family_id
    WHERE f.wife_id IS NOT NULL
);

WHILE FOUND IS TRUE LOOP
INSERT INTO ancestry (
    SELECT
        p.ancestor_id,
        c.descendant_id,
        CONCAT(COALESCE((p.via || ','),''), p.descendant_id, (COALESCE((',' || c.via), ''))) AS via,
        CONCAT(p.ancestor_id, ',', COALESCE((p.via || ','),''), p.descendant_id, (COALESCE((',' || c.via), '')), ',', c.descendant_id) AS path,
        p.distance + c.distance AS distance
    FROM ancestry AS p
    INNER JOIN ancestry AS c
        ON p.descendant_id = c.ancestor_id
    WHERE NOT EXISTS (
        SELECT * FROM ancestry AS t2
        WHERE t2.ancestor_id = p.ancestor_id
          AND t2.descendant_id = c.descendant_id
    )
) ON CONFLICT(ancestor_id, descendant_id) DO NOTHING;
END LOOP;

-- Insert all people as relatives of themselves with distance 0. This is important
-- for least common ancestor relationships, for otherwise we would miss parental
-- relationships.
INSERT INTO ancestry (
    SELECT p.id, p.id, NULL, NULL, 0 FROM persons p
);

END;
$$  LANGUAGE plpgsql;


