
DROP TABLE least_common_ancestors;

CREATE VIEW least_common_ancestors AS
SELECT
        a.descendant_id AS subject_1,
        b.descendant_id AS subject_2,
        a.ancestor_id AS least_common_ancestor,
        a.via AS subject_1_via,
        a.distance AS subject_1_distance,
        b.via AS subject_2_via,
        b.distance AS subject_2_distance
    FROM ancestry a INNER JOIN ancestry b
        ON a.ancestor_id = b.ancestor_id
    WHERE a.distance != 0
        AND b.distance != 0
        AND a.descendant_id != b.descendant_id
    ORDER BY
        (a.distance + b.distance),
        a.distance,
        b.distance;