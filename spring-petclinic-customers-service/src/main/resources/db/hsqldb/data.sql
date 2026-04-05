-- UUIDs are deterministic (uuid5-style names) so vets-service and customers stay aligned.
-- clinic/demo, clinic/demo2, owner/1..12, pet/1..15, vet/*, user/demo, user/demo2

INSERT INTO clinics (id, name, phone, address) VALUES
('78343a5e-047b-5edb-9975-678bf3f815c6', 'Demo Veterinary Clinic', '0280000000', 'Ho Chi Minh City'),
('f4b59806-b23b-598c-bf07-e3f87bb5cd99', 'Demo1 Veterinary Clinic', '0280000001', 'Da Nang City');

INSERT INTO clinic_users (id, clinic_id, email, password_hash, display_name, veterinarian_id, clinic_admin) VALUES
('1da081cb-aa57-5464-b3ec-b6ff37fa9abd', '78343a5e-047b-5edb-9975-678bf3f815c6', 'demo@clinic.vet',
 '$2b$12$HcueYdO4XiuOG4fF2eZecevjW2b8GAHY1XTbOWkgXiAheWiM/8UG6', 'Demo Veterinarian', '3891f3b9-e16a-5f0d-8307-da55b1fed172', TRUE),
('93316cbe-4bc9-5a27-9fd4-5ae09138b1ed', 'f4b59806-b23b-598c-bf07-e3f87bb5cd99', 'demo1@clinic.vet',
 '$2b$12$HcueYdO4XiuOG4fF2eZecevjW2b8GAHY1XTbOWkgXiAheWiM/8UG6', 'Demo1 Veterinarian', 'fbe4c768-d8b8-5854-92e0-2d9648793ea0', TRUE);

INSERT INTO types VALUES (1, 'cat');
INSERT INTO types VALUES (2, 'dog');

INSERT INTO owners (id, first_name, last_name, address, city, telephone, clinic_id) VALUES
('2ba55f38-4ab6-55d6-8011-ebc815a42531', 'George', 'Franklin', '110 W. Liberty St.', 'Madison', '6085551023', '78343a5e-047b-5edb-9975-678bf3f815c6'),
('53313d02-2ee2-5a51-8d36-1656be5479fa', 'Betty', 'Davis', '638 Cardinal Ave.', 'Sun Prairie', '6085551749', '78343a5e-047b-5edb-9975-678bf3f815c6'),
('deaa64fc-fabf-524e-8ba5-19191f308b2b', 'Eduardo', 'Rodriquez', '2693 Commerce St.', 'McFarland', '6085558763', '78343a5e-047b-5edb-9975-678bf3f815c6'),
('0a49975a-8fb7-5d0f-b79d-a66856cc36bb', 'Harold', 'Davis', '563 Friendly St.', 'Windsor', '6085553198', '78343a5e-047b-5edb-9975-678bf3f815c6'),
('ae181ada-0803-504a-9bba-9f0330ee7683', 'Peter', 'McTavish', '2387 S. Fair Way', 'Madison', '6085552765', '78343a5e-047b-5edb-9975-678bf3f815c6'),
('2635cb4f-6b62-5ea8-ba5d-125adab6eb1e', 'Jean', 'Coleman', '105 N. Lake St.', 'Monona', '6085552654', '78343a5e-047b-5edb-9975-678bf3f815c6'),
('76524dc5-9786-50ba-879d-af9790338f46', 'Jeff', 'Black', '1450 Oak Blvd.', 'Monona', '6085555387', '78343a5e-047b-5edb-9975-678bf3f815c6'),
('030de42b-9b24-5073-9a94-defa7104ae94', 'Maria', 'Escobito', '345 Maple St.', 'Madison', '6085557683', '78343a5e-047b-5edb-9975-678bf3f815c6'),
('28996ddb-930e-51b1-b96c-cba544eaa86a', 'David', 'Schroeder', '2749 Blackhawk Trail', 'Madison', '6085559435', '78343a5e-047b-5edb-9975-678bf3f815c6'),
('e5ee76b8-afde-5ac3-9086-689a6e2e5ac0', 'Carlos', 'Estaban', '2335 Independence La.', 'Waunakee', '6085555487', '78343a5e-047b-5edb-9975-678bf3f815c6'),
('a2b4049e-9eb8-5fa0-9121-d78e51ff4023', 'Lan', 'Nguyen', '123 Bach Dang St.', 'Da Nang', '+84 02363550001', 'f4b59806-b23b-598c-bf07-e3f87bb5cd99'),
('b78f3755-0da6-5f4d-952f-244450b3a590', 'Minh', 'Tran', '45 Hai Chau Ward', 'Da Nang', '+84 02363550002', 'f4b59806-b23b-598c-bf07-e3f87bb5cd99');

INSERT INTO pets (id, name, birth_date, type_id, owner_id, gender, vaccination_status, medical_notes) VALUES
('f4dad75f-eebf-549f-8459-4ef9798de80f', 'Leo', '2010-09-07', 1, '2ba55f38-4ab6-55d6-8011-ebc815a42531', 'male', 'yes', 'Regular checkups'),
('6cd5ce45-69f2-50ee-a996-db54a21c49c0', 'Basil', '2012-08-06', 1, '53313d02-2ee2-5a51-8d36-1656be5479fa', 'male', 'yes', 'Indoor cat'),
('08a41dde-6aa3-5082-ba44-abb5c876d4ec', 'Rosy', '2011-04-17', 2, 'deaa64fc-fabf-524e-8ba5-19191f308b2b', 'female', 'yes', 'Active dog'),
('9dc49332-7257-5b77-8bcb-59e1f65828d2', 'Jewel', '2000-03-07', 1, 'deaa64fc-fabf-524e-8ba5-19191f308b2b', 'female', 'yes', 'Playful kitten'),
('dee9aa5c-f228-54fc-b402-76e74422965b', 'Iggy', '2000-11-30', 2, '0a49975a-8fb7-5d0f-b79d-a66856cc36bb', 'male', 'no', 'Young small dog'),
('7b122684-1c59-50b2-89a4-3df27162d59d', 'George', '2000-01-20', 1, 'ae181ada-0803-504a-9bba-9f0330ee7683', 'male', 'yes', 'Senior cat'),
('5f9e4451-c48e-5b1c-a7dc-806cfa96e42c', 'Samantha', '1995-09-04', 2, '2635cb4f-6b62-5ea8-ba5d-125adab6eb1e', 'female', 'yes', 'Golden retriever'),
('f17ee58b-e9e1-5908-a358-73c2c0a9aaf7', 'Max', '1995-09-04', 2, '2635cb4f-6b62-5ea8-ba5d-125adab6eb1e', 'male', 'yes', 'Labrador mix'),
('653287cf-5262-5707-8db4-404c1949298e', 'Lucky', '1999-08-06', 1, '76524dc5-9786-50ba-879d-af9790338f46', 'female', 'no', 'Domestic short hair'),
('edfa3f60-cfc7-5dc8-a5c4-7574af6f4612', 'Mulligan', '1997-02-24', 2, '030de42b-9b24-5073-9a94-defa7104ae94', 'male', 'yes', 'Terrier mix'),
('e515f5cd-da66-580b-a234-db68c7e5130a', 'Freddy', '2000-03-09', 1, '28996ddb-930e-51b1-b96c-cba544eaa86a', 'male', 'no', 'Indoor kitten'),
('84ba6986-11e4-552f-965d-52f772096236', 'Lucky', '2000-06-24', 1, 'e5ee76b8-afde-5ac3-9086-689a6e2e5ac0', 'female', 'yes', 'Siamese cat'),
('5b99a21d-4b78-5d37-ab34-bd709265d1a7', 'Sly', '2002-06-08', 1, 'e5ee76b8-afde-5ac3-9086-689a6e2e5ac0', 'male', 'yes', 'Outdoor cat'),
('3b4e3eb7-dfc8-54f0-8092-2e6424f862e6', 'Milo', '2019-05-10', 1, 'a2b4049e-9eb8-5fa0-9121-d78e51ff4023', 'male', 'yes', 'Demo1 clinic — indoor cat'),
('4680ef1d-e020-56ae-adf4-a5c211391a2e', 'Bella', '2020-03-15', 2, 'b78f3755-0da6-5f4d-952f-244450b3a590', 'female', 'yes', 'Demo1 clinic — checkups');
