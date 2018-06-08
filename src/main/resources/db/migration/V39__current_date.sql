
CREATE TABLE check_date (
  last_check_date     DATE NOT NULL
);

-- Ensure there can only be one row in this table
CREATE UNIQUE INDEX config_single_row ON check_date ((last_check_date IS NOT NULL));
