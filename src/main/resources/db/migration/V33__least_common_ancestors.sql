CREATE OR REPLACE VIEW least_common_ancestors AS
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
  ORDER BY
    (a.distance + b.distance),
    a.distance,
    b.distance;
