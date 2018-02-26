CREATE TABLE names_first (
    name TEXT NOT NULL,
    gender CHAR(1) NOT NULL,
    order SMALLINT,
    weight NUMERIC NOT NULL,
    PRIMARY KEY(gender, name)
);

CREATE TABLE names_last (
    name TEXT PRIMARY KEY
);

INSERT INTO names_first (order, name, gender, weight) VALUES

(1, 'William', 'M', 1),
(2, 'John', 'M', 0.95),
(3, 'Thomas', 'M', 0.9025),
(4, 'James', 'M', 0.857375),
(5, 'George', 'M', 0.81450625),
(6, 'Joseph', 'M', 0.7737809375),
(7, 'Richard', 'M', 0.735091890625),
(8, 'Henry', 'M', 0.69833729609375),
(9, 'Robert', 'M', 0.663420431289062),
(10, 'Charles', 'M', 0.630249409724609),
(11, 'Samuel', 'M', 0.598736939238379),
(12, 'Edward', 'M', 0.56880009227646),
(13, 'Benjamin', 'M', 0.540360087662637),
(14, 'Isaac', 'M', 0.513342083279505),
(15, 'Peter', 'M', 0.487674979115529),
(16, 'Daniel', 'M', 0.463291230159753),
(17, 'David', 'M', 0.440126668651765),
(18, 'Francis', 'M', 0.418120335219177),
(19, 'Stephen', 'M', 0.397214318458218),
(20, 'Jonathan', 'M', 0.377353602535307),
(21, 'Christopher', 'M', 0.358485922408542),
(22, 'Matthew', 'M', 0.340561626288115),
(23, 'Edmund', 'M', 0.323533544973709),
(24, 'Philip', 'M', 0.307356867725023),
(25, 'Abraham', 'M', 0.291989024338772),
(26, 'Mark', 'M', 0.277389573121834),
(27, 'Michael', 'M', 0.263520094465742),
(28, 'Ralph', 'M', 0.250344089742455),
(29, 'Jacob', 'M', 0.237826885255332),
(30, 'Andrew', 'M', 0.225935540992565),
(31, 'Moses', 'M', 0.214638763942937),
(32, 'Nicholas', 'M', 0.20390682574579),
(33, 'Anthony', 'M', 0.193711484458501),
(34, 'Luke', 'M', 0.184025910235576),
(35, 'Simon', 'M', 0.174824614723797),
(36, 'Josiah', 'M', 0.166083383987607),
(37, 'Timothy', 'M', 0.157779214788227),
(38, 'Martin', 'M', 0.149890254048815),
(39, 'Nathaniel', 'M', 0.142395741346375),
(40, 'Roger', 'M', 0.135275954279056),
(41, 'Walter', 'M', 0.128512156565103),
(42, 'Aaron', 'M', 0.122086548736848),
(43, 'Jeremy', 'M', 0.115982221300006),
(44, 'Joshua', 'M', 0.110183110235005),
(45, 'Alexander', 'M', 0.104673954723255),
(46, 'Adam', 'M', 0.099440256987092),
(47, 'Hugh', 'M', 0.094468244137738),
(48, 'Laurence', 'M', 0.089744831930851),
(49, 'Owen', 'M', 0.085257590334308),
(50, 'Harry', 'M', 0.080994710817593),
(1, 'Mary', 'F', 1),
(2, 'Anne', 'F', 0.95),
(3, 'Elizabeth', 'F', 0.9025),
(4, 'Sarah', 'F', 0.857375),
(5, 'Jane', 'F', 0.81450625),
(6, 'Hannah', 'F', 0.7737809375),
(7, 'Susan', 'F', 0.735091890625),
(8, 'Martha', 'F', 0.69833729609375),
(9, 'Margaret', 'F', 0.663420431289062),
(10, 'Charlotte', 'F', 0.630249409724609),
(11, 'Harriet', 'F', 0.598736939238379),
(12, 'Betty', 'F', 0.56880009227646),
(13, 'Maria', 'F', 0.540360087662637),
(14, 'Catherine', 'F', 0.513342083279505),
(15, 'Frances', 'F', 0.487674979115529),
(16, 'Mary Ann', 'F', 0.463291230159753),
(17, 'Nancy', 'F', 0.440126668651765),
(18, 'Rebecca', 'F', 0.418120335219177),
(19, 'Alice', 'F', 0.397214318458218),
(20, 'Ellen', 'F', 0.377353602535307),
(21, 'Sophia', 'F', 0.358485922408542),
(22, 'Lucy', 'F', 0.340561626288115),
(23, 'Isabel', 'F', 0.323533544973709),
(24, 'Eleanor', 'F', 0.307356867725023),
(25, 'Esther', 'F', 0.291989024338772),
(26, 'Fanny', 'F', 0.277389573121834),
(27, 'Eliza', 'F', 0.263520094465742),
(28, 'Grace', 'F', 0.250344089742455),
(29, 'Sally', 'F', 0.237826885255332),
(30, 'Rachel', 'F', 0.225935540992565),
(31, 'Lydia', 'F', 0.214638763942937),
(32, 'Caroline', 'F', 0.20390682574579),
(33, 'Dorothy', 'F', 0.193711484458501),
(34, 'Peggy', 'F', 0.184025910235576),
(35, 'Ruth', 'F', 0.174824614723797),
(36, 'Kitty', 'F', 0.166083383987607),
(37, 'Jenny', 'F', 0.157779214788227),
(38, 'Phoebe', 'F', 0.149890254048815),
(39, 'Agnes', 'F', 0.142395741346375),
(40, 'Emma', 'F', 0.135275954279056),
(41, 'Amy', 'F', 0.128512156565103),
(42, 'Jemima', 'F', 0.122086548736848),
(43, 'Dinah', 'F', 0.115982221300006),
(44, 'Barbara', 'F', 0.110183110235005),
(45, 'Joan', 'F', 0.104673954723255),
(46, 'Joanna', 'F', 0.099440256987092),
(47, 'Deborah', 'F', 0.094468244137738),
(48, 'Judith', 'F', 0.089744831930851),
(49, 'Bridget', 'F', 0.085257590334308),
(50, 'Marjorie', 'F', 0.080994710817593),
(null, 'Adolphus', 'M', 0.002),
(null, 'Allan', 'M', 0.002),
(null, 'Ambrose', 'M', 0.002),
(null, 'Augustus', 'M', 0.002),
(null, 'Bartholomew', 'M', 0.002),
(null, 'Cornelius', 'M', 0.002),
(null, 'Cuthbert', 'M', 0.002),
(null, 'Ebenezer', 'M', 0.002),
(null, 'Erasmus', 'M', 0.002),
(null, 'Ernest', 'M', 0.002),
(null, 'Frederick', 'M', 0.002),
(null, 'Gabriel', 'M', 0.002),
(null, 'Giles', 'M', 0.002),
(null, 'Horatio', 'M', 0.002),
(null, 'Humphrey', 'M', 0.002),
(null, 'Jasper', 'M', 0.002),
(null, 'Jeremiah', 'M', 0.002),
(null, 'Job', 'M', 0.002),
(null, 'Jonas', 'M', 0.002),
(null, 'Joseph', 'M', 0.002),
(null, 'Josiah', 'M', 0.002),
(null, 'Leonard', 'M', 0.002),
(null, 'Louis', 'M', 0.002),
(null, 'Luke', 'M', 0.002),
(null, 'Marmaduke', 'M', 0.002),
(null, 'Miles', 'M', 0.002),
(null, 'Nathan', 'M', 0.002),
(null, 'Octavius', 'M', 0.002),
(null, 'Simeon', 'M', 0.002),
(null, 'Solomon', 'M', 0.002),
(null, 'Theophilus', 'M', 0.002),
(null, 'Valentine', 'M', 0.002),
(null, 'William', 'M', 0.002),
(null, 'Adelaide', 'F', 0.002),
(null, 'Amelia', 'F', 0.002),
(null, 'Arabella', 'F', 0.002),
(null, 'Augusta', 'F', 0.002),
(null, 'Cecilia', 'F', 0.002),
(null, 'Christiana', 'F', 0.002),
(null, 'Clara', 'F', 0.002),
(null, 'Diana', 'F', 0.002),
(null, 'Dorothea', 'F', 0.002),
(null, 'Emily', 'F', 0.002),
(null, 'Euphemia', 'F', 0.002),
(null, 'Georgiana', 'F', 0.002),
(null, 'Helena', 'F', 0.002),
(null, 'Henrietta', 'F', 0.002),
(null, 'Hester', 'F', 0.002),
(null, 'Isabella', 'F', 0.002),
(null, 'Joanna', 'F', 0.002),
(null, 'Julia', 'F', 0.002),
(null, 'Keziah', 'F', 0.002),
(null, 'Lavinia', 'F', 0.002),
(null, 'Louisa', 'F', 0.002),
(null, 'Margery', 'F', 0.002),
(null, 'Marianne', 'F', 0.002),
(null, 'Mary', 'F', 0.002),
(null, 'Matilda', 'F', 0.002),
(null, 'Mercy', 'F', 0.002),
(null, 'Olivia', 'F', 0.002),
(null, 'Patience', 'F', 0.002),
(null, 'Philadelphia', 'F', 0.002),
(null, 'Selina', 'F', 0.002),
(null, 'Susanna', 'F', 0.002),
(null, 'Theodosia', 'F', 0.002),
(null, 'Wilhelmina', 'F', 0.002),
(null, 'Abigail', 'F', 0.002),
(null, 'Albina', 'F', 0.002),
(null, 'Alicia', 'F', 0.002),
(null, 'Angel', 'F', 0.002),
(null, 'Anna', 'F', 0.002),
(null, 'Ann', 'F', 0.002),
(null, 'Awellah', 'F', 0.002),
(null, 'Beatrice', 'F', 0.002),
(null, 'Betsey', 'F', 0.002),
(null, 'Katherine', 'F', 0.002),
(null, 'Charity', 'F', 0.002),
(null, 'Christianna', 'F', 0.002),
(null, 'Edith', 'F', 0.002),
(null, 'Emmeline', 'F', 0.002),
(null, 'Florentia', 'F', 0.002),
(null, 'Frederica', 'F', 0.002),
(null, 'Georgina', 'F', 0.002),
(null, 'Helen', 'F', 0.002),
(null, 'Honora', 'F', 0.002),
(null, 'Horatia', 'F', 0.002),
(null, 'Isabella', 'F', 0.002),
(null, 'Jean', 'F', 0.002),
(null, 'Jessie', 'F', 0.002),
(null, 'Joanna', 'F', 0.002),
(null, 'Joyce', 'F', 0.002),
(null, 'Juliana', 'F', 0.002),
(null, 'Juliet', 'F', 0.002),
(null, 'Laura', 'F', 0.002),
(null, 'Leah', 'F', 0.002),
(null, 'Letitia', 'F', 0.002),
(null, 'Lilias', 'F', 0.002),
(null, 'Louisa', 'F', 0.002),
(null, 'Louisa-Margaretta', 'F', 0.002),
(null, 'Lucy-Anne', 'F', 0.002),
(null, 'Madalene', 'F', 0.002),
(null, 'Marianne', 'F', 0.002),
(null, 'Marina', 'F', 0.002),
(null, 'Ann', 'F', 0.002),
(null, 'Mary-Anne', 'F', 0.002),
(null, 'Miriam', 'F', 0.002),
(null, 'Modesty', 'F', 0.002),
(null, 'Peace', 'F', 0.002),
(null, 'Phillis', 'F', 0.002),
(null, 'Phyllis', 'F', 0.002),
(null, 'Priscilla', 'F', 0.002),
(null, 'Prudence', 'F', 0.002),
(null, 'Rose', 'F', 0.002),
(null, 'Susanna', 'F', 0.002),
(null, 'Susannah', 'F', 0.002),
(null, 'Tabitha', 'F', 0.002),
(null, 'Teresa', 'F', 0.002),
(null, 'Unity', 'F', 0.002),
(null, 'Albinus', 'M', 0.002),
(null, 'Albion', 'M', 0.002),
(null, 'Algernon', 'M', 0.002),
(null, 'Americus', 'M', 0.002),
(null, 'Archibald', 'M', 0.002),
(null, 'Arthur', 'M', 0.002),
(null, 'Aylmer', 'M', 0.002),
(null, 'Baldwin', 'M', 0.002),
(null, 'Barnard', 'M', 0.002),
(null, 'Benedict', 'M', 0.002),
(null, 'Brook', 'M', 0.002),
(null, 'Carew', 'M', 0.002),
(null, 'Cecil', 'M', 0.002),
(null, 'Christmas', 'M', 0.002),
(null, 'Coape', 'M', 0.002),
(null, 'Colin', 'M', 0.002),
(null, 'Donald', 'M', 0.002),
(null, 'Dudley', 'M', 0.002),
(null, 'Duncan', 'M', 0.002),
(null, 'Edwin', 'M', 0.002),
(null, 'Eli', 'M', 0.002),
(null, 'Elias', 'M', 0.002),
(null, 'Emanuel', 'M', 0.002),
(null, 'Ephraim', 'M', 0.002),
(null, 'Evan', 'M', 0.002),
(null, 'Ewan', 'M', 0.002),
(null, 'Ezra', 'M', 0.002),
(null, 'Felton', 'M', 0.002),
(null, 'Gerard', 'M', 0.002),
(null, 'Gibbs', 'M', 0.002),
(null, 'Gilbert', 'M', 0.002),
(null, 'Graham', 'M', 0.002),
(null, 'Guy', 'M', 0.002),
(null, 'Harcourt', 'M', 0.002),
(null, 'Herbert', 'M', 0.002),
(null, 'Honor', 'M', 0.002),
(null, 'Horace', 'M', 0.002),
(null, 'Hudson', 'M', 0.002),
(null, 'Jahleel', 'M', 0.002),
(null, 'Jeffrey', 'M', 0.002),
(null, 'Jerome', 'M', 0.002),
(null, 'Josias', 'M', 0.002),
(null, 'Kenneth', 'M', 0.002),
(null, 'Levi', 'M', 0.002),
(null, 'Lewis', 'M', 0.002),
(null, 'Lodge', 'M', 0.002),
(null, 'Loftus', 'M', 0.002),
(null, 'Ludlow', 'M', 0.002),
(null, 'Meshach', 'M', 0.002),
(null, 'Morgan', 'M', 0.002),
(null, 'Nash', 'M', 0.002),
(null, 'Neil', 'M', 0.002),
(null, 'Noah', 'M', 0.002),
(null, 'Norman', 'M', 0.002),
(null, 'Obadiah', 'M', 0.002),
(null, 'Oliver', 'M', 0.002),
(null, 'Patrick', 'M', 0.002),
(null, 'Percy', 'M', 0.002),
(null, 'Percival', 'M', 0.002),
(null, 'Peregrine', 'M', 0.002),
(null, 'Phineas', 'M', 0.002),
(null, 'Reginald', 'M', 0.002),
(null, 'Reuben', 'M', 0.002),
(null, 'Rollo', 'M', 0.002),
(null, 'Sampson', 'M', 0.002),
(null, 'Seth', 'M', 0.002),
(null, 'Shadrack', 'M', 0.002),
(null, 'Sherborne', 'M', 0.002),
(null, 'Silas', 'M', 0.002),
(null, 'Theophile', 'M', 0.002);

INSERT INTO names_last (name) VALUES
('Agar'),
('Allen'),
('Andrews'),
('Arnold'),
('Ayles'),
('Balfour'),
('Baker'),
('Bannerman'),
('Banfield'),
('Barnes'),
('Bamber'),
('Barnet'),
('Barrington'),
('Bartlett'),
('Barton'),
('Bastable'),
('Baxter'),
('Bolt'),
('Bragg'),
('Brown'),
('Beaumont'),
('Beauchamp'),
('Birks'),
('Blackmore'),
('Bolton'),
('Bond'),
('Booth'),
('Bowles'),
('Boyle'),
('Burk'),
('Butler'),
('Buxton'),
('Brooks'),
('Browning'),
('Byrd'),
('Caddy'),
('Campbell'),
('Caney'),
('Carter'),
('Chant'),
('Clark'),
('Clarke'),
('Cluett'),
('Colborne'),
('Comerford'),
('Conolly'),
('Cooke'),
('Coombs'),
('Cooper'),
('Crampton'),
('Crauford'),
('Creassey'),
('Crew'),
('Cull'),
('Curtis'),
('Davis'),
('Daw'),
('Dean'),
('Dennis'),
('Dodge'),
('Dowding'),
('Down'),
('Drake'),
('Drew'),
('Dunn'),
('Dyke'),
('Edge'),
('Egerton'),
('Elkins'),
('Ellis'),
('Fagean'),
('Fernside'),
('Fifett'),
('Filliol'),
('Finch'),
('Fitzgerald'),
('Fitzroy'),
('Fitzwilliam'),
('Fletcher'),
('Follett'),
('Forbes'),
('Fortescue'),
('Frampton'),
('Fudge'),
('Gale'),
('Galpin'),
('Garvey'),
('George'),
('Gibbon'),
('Gibbs'),
('Gillett'),
('Gillingham'),
('Godwin'),
('Grant'),
('Grave'),
('Green'),
('Gregory'),
('Grove'),
('Gouldsmith'),
('Gulliver'),
('Guppy'),
('Guy'),
('Haddington'),
('Hancock'),
('Hann'),
('Hardy'),
('Harding'),
('Harris'),
('Haskett'),
('Hayward'),
('Hatcher'),
('Hawkins'),
('Herbert'),
('Hervey'),
('Hill'),
('Hind'),
('Hodge'),
('Honeyfield'),
('Horder'),
('Hose'),
('Hoskins'),
('Hosmer'),
('Howe'),
('Hughes'),
('Humphries'),
('Hunt'),
('Hunter'),
('Huntington'),
('Hyatt'),
('Jarvis'),
('Jennings'),
('Johnson'),
('Jones'),
('Keats'),
('Kendall'),
('Kingman'),
('Knight'),
('Lambert'),
('Langley'),
('Leeson'),
('Lewis'),
('Lightholder'),
('Lilley'),
('Linfield'),
('Livingston'),
('Lock'),
('Lockhart'),
('Longman'),
('Lovell'),
('Lush'),
('Lymington'),
('Major'),
('Mead'),
('Miash'),
('Merchant'),
('Miller'),
('Montgomery'),
('Montagu'),
('Moore'),
('Morton'),
('Mowatt'),
('Mowbray'),
('Mullens'),
('Munro'),
('Neal'),
('Needs'),
('Nicholson'),
('Nightingale'),
('Norman'),
('Notley'),
('Nott'),
('Oakley'),
('Oliver'),
('Parsons'),
('Pemberton'),
('Pembroke'),
('Perry'),
('Pike'),
('Place'),
('Plowman'),
('Powell'),
('Pratt'),
('Price'),
('Radcliff'),
('Rake'),
('Ramsbury'),
('Rayment'),
('Read'),
('Reid'),
('Reeves'),
('Repington'),
('Richards'),
('Ridlington'),
('Ridout'),
('Robins'),
('Rowe'),
('Rowley'),
('Rutley'),
('Ruteledge'),
('Russ'),
('Russell'),
('Scriven'),
('Sculthorpe'),
('Sedgewick'),
('Selkirk'),
('Sempill'),
('Sergeant'),
('Seton'),
('Seymour'),
('Sharp'),
('Shepherd'),
('Sims'),
('Sinclair'),
('Skeffington'),
('Skinner'),
('Slade'),
('Slyfeel'),
('Snowley'),
('Soulden'),
('Spranklin'),
('St. John'),
('St. George'),
('St. Vincent'),
('St. Clair'),
('Stanhope'),
('Stanton'),
('Stapleton'),
('Stewart'),
('Storey'),
('Sullyard'),
('Stay'),
('Stewart'),
('Stickland'),
('Stone'),
('Sweet'),
('Swinton'),
('Talbot'),
('Tattershall'),
('Templeton'),
('Terrell'),
('Thorne'),
('Thrup'),
('Townshend'),
('Trew'),
('Tulk'),
('Turner'),
('Trowbridge'),
('Vaughan'),
('Vivian'),
('Voss'),
('Walford'),
('Wallace'),
('Ware'),
('Warren'),
('Warwick'),
('Watts'),
('Webb'),
('Weston'),
('Windham'),
('White'),
('Wilds'),
('Wilmington'),
('Wilson'),
('Wood'),
('Worthington'),
('Wright'),
('Wynn'),
('Wycliff'),
('Yeatman'),
('Young');
