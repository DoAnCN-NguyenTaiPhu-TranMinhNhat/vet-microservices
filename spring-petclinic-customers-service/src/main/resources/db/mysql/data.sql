INSERT IGNORE INTO types VALUES (1, 'cat');
INSERT IGNORE INTO types VALUES (2, 'dog');
INSERT IGNORE INTO types VALUES (3, 'lizard');
INSERT IGNORE INTO types VALUES (4, 'snake');
INSERT IGNORE INTO types VALUES (5, 'bird');
INSERT IGNORE INTO types VALUES (6, 'hamster');

INSERT IGNORE INTO owners VALUES (1, 'George', 'Franklin', '110 W. Liberty St.', 'Madison', '6085551023');
INSERT IGNORE INTO owners VALUES (2, 'Betty', 'Davis', '638 Cardinal Ave.', 'Sun Prairie', '6085551749');
INSERT IGNORE INTO owners VALUES (3, 'Eduardo', 'Rodriquez', '2693 Commerce St.', 'McFarland', '6085558763');
INSERT IGNORE INTO owners VALUES (4, 'Harold', 'Davis', '563 Friendly St.', 'Windsor', '6085553198');
INSERT IGNORE INTO owners VALUES (5, 'Peter', 'McTavish', '2387 S. Fair Way', 'Madison', '6085552765');
INSERT IGNORE INTO owners VALUES (6, 'Jean', 'Coleman', '105 N. Lake St.', 'Monona', '6085552654');
INSERT IGNORE INTO owners VALUES (7, 'Jeff', 'Black', '1450 Oak Blvd.', 'Monona', '6085555387');
INSERT IGNORE INTO owners VALUES (8, 'Maria', 'Escobito', '345 Maple St.', 'Madison', '6085557683');
INSERT IGNORE INTO owners VALUES (9, 'David', 'Schroeder', '2749 Blackhawk Trail', 'Madison', '6085559435');
INSERT IGNORE INTO owners VALUES (10, 'Carlos', 'Estaban', '2335 Independence La.', 'Waunakee', '6085555487');

INSERT IGNORE INTO pets (name, birth_date, type_id, owner_id, gender, vaccination_status, medical_notes) VALUES 
('Leo', '2010-09-07', 1, 1, 'male', 'yes', 'Regular checkups'),
('Basil', '2012-08-06', 6, 2, 'male', 'yes', 'Indoor cat'),
('Rosy', '2011-04-17', 2, 3, 'female', 'yes', 'Active dog'),
('Jewel', '2000-03-07', 2, 3, 'female', 'yes', 'Playful kitten'),
('Iggy', '2000-11-30', 3, 4, 'male', 'no', 'Young lizard'),
('George', '2000-01-20', 4, 5, 'male', 'yes', 'Senior cat'),
('Samantha', '1995-09-04', 1, 6, 'female', 'yes', 'Golden retriever'),
('Max', '1995-09-04', 1, 6, 'male', 'yes', 'Labrador mix'),
('Lucky', '1999-08-06', 5, 7, 'female', 'no', 'Rabbit'),
('Mulligan', '1997-02-24', 2, 8, 'male', 'yes', 'Terrier mix'),
('Freddy', '2000-03-09', 5, 9, 'male', 'no', 'Hamster'),
('Lucky', '2000-06-24', 2, 10, 'female', 'yes', 'Siamese cat'),
('Sly', '2002-06-08', 1, 10, 'male', 'yes', 'Outdoor cat');
