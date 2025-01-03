ALTER TABLE occupations ADD COLUMN may_marry BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE occupations ADD COLUMN min_income_required DOUBLE PRECISION;
ALTER TABLE occupations ADD COLUMN max_per_household INT NOT NULL DEFAULT 0;

DELETE FROM occupations WHERE name = 'Domestic servant';

INSERT INTO occupations (support_factor, name, allow_male, allow_female, min_class, max_class,
                         is_domestic_servant, may_marry, min_income_required, max_per_household) VALUES
  (0, 'Steward', TRUE, FALSE, 'YEOMAN_OR_MERCHANT', 'YEOMAN_OR_MERCHANT', TRUE, TRUE, 4000, 1),
  (0, 'Butler', TRUE, FALSE, 'YEOMAN_OR_MERCHANT', 'YEOMAN_OR_MERCHANT', TRUE, TRUE, 2000, 1),
  (0, 'Housekeeper', FALSE, TRUE, 'LANDOWNER_OR_CRAFTSMAN', 'YEOMAN_OR_MERCHANT', TRUE, FALSE, 1500, 1),
  (0, 'Cook', TRUE, TRUE, 'LANDOWNER_OR_CRAFTSMAN', 'YEOMAN_OR_MERCHANT', TRUE, FALSE, 400, 1),
  (0, 'Valet', TRUE, FALSE, 'YEOMAN_OR_MERCHANT', 'YEOMAN_OR_MERCHANT', TRUE, FALSE, 2000, 1),
  (0, 'Lady''s Maid', FALSE, TRUE, 'LANDOWNER_OR_CRAFTSMAN', 'YEOMAN_OR_MERCHANT', TRUE, FALSE, 2000, 1),
  (0, 'Nurse', FALSE, TRUE, 'LANDOWNER_OR_CRAFTSMAN', 'LANDOWNER_OR_CRAFTSMAN', TRUE, FALSE, 2000, 1),
  (0, 'Housemaid', FALSE, TRUE, 'LABORER', 'LANDOWNER_OR_CRAFTSMAN', TRUE, FALSE, 400, 2),
  (0, 'Maid', FALSE, TRUE, 'PAUPER', 'LABORER', TRUE, FALSE, 100, 6),
  (0, 'Coachman', TRUE, FALSE, 'LANDOWNER_OR_CRAFTSMAN', 'LANDOWNER_OR_CRAFTSMAN', TRUE, FALSE, 1000, 1),
  (0, 'Groom', TRUE, FALSE, 'LABORER', 'LANDOWNER_OR_CRAFTSMAN', TRUE, FALSE, 500, 3),
  (0, 'Footman', TRUE, FALSE, 'LABORER', 'LANDOWNER_OR_CRAFTSMAN', TRUE, FALSE, 600, 2);
