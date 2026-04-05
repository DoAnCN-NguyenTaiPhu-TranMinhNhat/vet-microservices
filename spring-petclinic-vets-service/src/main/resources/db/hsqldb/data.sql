INSERT INTO vets (id, first_name, last_name, clinic_id) VALUES ('3891f3b9-e16a-5f0d-8307-da55b1fed172', 'James', 'Carter', '78343a5e-047b-5edb-9975-678bf3f815c6');
INSERT INTO vets (id, first_name, last_name, clinic_id) VALUES ('fbe4c768-d8b8-5854-92e0-2d9648793ea0', 'Helen', 'Leary', 'f4b59806-b23b-598c-bf07-e3f87bb5cd99');
INSERT INTO vets (id, first_name, last_name, clinic_id) VALUES ('2cc799c9-82e9-5736-9768-a44a08cd6562', 'Linda', 'Douglas', '78343a5e-047b-5edb-9975-678bf3f815c6');
INSERT INTO vets (id, first_name, last_name, clinic_id) VALUES ('8659a563-0087-5717-a9ca-98b7a837b1b3', 'Rafael', 'Ortega', '78343a5e-047b-5edb-9975-678bf3f815c6');
INSERT INTO vets (id, first_name, last_name, clinic_id) VALUES ('6f10327e-22d0-5a54-bc97-6039f4b5036a', 'Henry', 'Stevens', 'f4b59806-b23b-598c-bf07-e3f87bb5cd99');
INSERT INTO vets (id, first_name, last_name, clinic_id) VALUES ('79565c22-891f-5bc3-9114-6066ccf6289d', 'Sharon', 'Jenkins', 'f4b59806-b23b-598c-bf07-e3f87bb5cd99');

INSERT INTO specialties VALUES (1, 'radiology');
INSERT INTO specialties VALUES (2, 'surgery');
INSERT INTO specialties VALUES (3, 'dentistry');

INSERT INTO vet_specialties VALUES ('fbe4c768-d8b8-5854-92e0-2d9648793ea0', 1);
INSERT INTO vet_specialties VALUES ('2cc799c9-82e9-5736-9768-a44a08cd6562', 2);
INSERT INTO vet_specialties VALUES ('2cc799c9-82e9-5736-9768-a44a08cd6562', 3);
INSERT INTO vet_specialties VALUES ('8659a563-0087-5717-a9ca-98b7a837b1b3', 2);
INSERT INTO vet_specialties VALUES ('6f10327e-22d0-5a54-bc97-6039f4b5036a', 1);
