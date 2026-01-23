INSERT INTO types VALUES (1, 'cat');
INSERT INTO types VALUES (2, 'dog');
INSERT INTO types VALUES (3, 'lizard');
INSERT INTO types VALUES (4, 'snake');
INSERT INTO types VALUES (5, 'bird');
INSERT INTO types VALUES (6, 'hamster');

INSERT INTO owners VALUES (1, 'George', 'Franklin', '110 W. Liberty St.', 'Madison', '6085551023');
INSERT INTO owners VALUES (2, 'Betty', 'Davis', '638 Cardinal Ave.', 'Sun Prairie', '6085551749');
INSERT INTO owners VALUES (3, 'Eduardo', 'Rodriquez', '2693 Commerce St.', 'McFarland', '6085558763');
INSERT INTO owners VALUES (4, 'Harold', 'Davis', '563 Friendly St.', 'Windsor', '6085553198');
INSERT INTO owners VALUES (5, 'Peter', 'McTavish', '2387 S. Fair Way', 'Madison', '6085552765');
INSERT INTO owners VALUES (6, 'Jean', 'Coleman', '105 N. Lake St.', 'Monona', '6085552654');
INSERT INTO owners VALUES (7, 'Jeff', 'Black', '1450 Oak Blvd.', 'Monona', '6085555387');
INSERT INTO owners VALUES (8, 'Maria', 'Escobito', '345 Maple St.', 'Madison', '6085557683');
INSERT INTO owners VALUES (9, 'David', 'Schroeder', '2749 Blackhawk Trail', 'Madison', '6085559435');
INSERT INTO owners VALUES (10, 'Carlos', 'Estaban', '2335 Independence La.', 'Waunakee', '6085555487');

INSERT INTO pets (name, birth_date, type_id, owner_id, gender, vaccination_status, medical_notes) VALUES 
('Leo', '2010-09-07', 1, 1, 'male', 'yes', 'Regular checkups'),
('Basil', '2012-08-06', 6, 2, 'male', 'yes', 'Indoor cat'),
('Rosy', '2011-04-17', 2, 3, 'female', 'yes', 'Active dog'),
('Jewel', '2010-03-07', 2, 3, 'female', 'yes', 'Sister of Rosy'),
('Iggy', '2010-11-30', 3, 4, 'male', 'unknown', 'Exotic pet'),
('George', '2010-01-20', 4, 5, 'male', 'no', 'First visit'),
('Samantha', '2012-09-04', 1, 6, 'female', 'yes', 'Indoor cat'),
('Max', '2012-09-04', 1, 6, 'male', 'yes', 'Brother of Samantha'),
('Lucky', '2011-08-06', 5, 7, 'male', 'yes', 'Healthy bird'),
('Mulligan', '2007-02-24', 2, 8, 'male', 'yes', 'Senior dog'),
('Freddy', '2010-03-09', 5, 9, 'male', 'no', 'Rescue bird'),
('Lucky', '2010-06-24', 2, 10, 'female', 'yes', 'Active dog'),
('Sly', '2012-06-08', 1, 10, 'male', 'yes', 'Indoor cat');
