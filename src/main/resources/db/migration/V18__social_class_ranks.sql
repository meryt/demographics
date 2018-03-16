
CREATE TABLE social_classes (
    id       TEXT PRIMARY KEY,
    rank     smallint
);

INSERT INTO social_classes (id, rank) VALUES
    ('PAUPER', 1),
    ('LABORER', 2),
    ('LANDOWNER_OR_CRAFTSMAN', 3),
    ('YEOMAN_OR_MERCHANT', 4),
    ('GENTLEMAN', 5),
    ('BARONET', 6),
    ('BARON', 7),
    ('VISCOUNT', 8),
    ('EARL', 9),
    ('MARQUESS', 10),
    ('DUKE', 11),
    ('PRINCE', 12),
    ('MONARCH', 13);

