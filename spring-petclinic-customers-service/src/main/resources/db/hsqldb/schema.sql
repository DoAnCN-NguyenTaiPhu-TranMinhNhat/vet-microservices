DROP TABLE clinic_users IF EXISTS;
DROP TABLE pets IF EXISTS;
DROP TABLE owners IF EXISTS;
DROP TABLE clinics IF EXISTS;
DROP TABLE types IF EXISTS;
DROP SEQUENCE owner_sequence IF EXISTS;
DROP SEQUENCE pet_sequence IF EXISTS;

CREATE TABLE types (
  id   INTEGER IDENTITY PRIMARY KEY,
  name VARCHAR(80)
);
CREATE INDEX types_name ON types (name);

CREATE TABLE clinics (
  id         VARCHAR(36) PRIMARY KEY,
  name       VARCHAR(120) NOT NULL,
  phone      VARCHAR(30),
  address    VARCHAR(255)
);
CREATE INDEX clinics_name ON clinics (name);

CREATE TABLE owners (
  id         VARCHAR(36) PRIMARY KEY,
  first_name VARCHAR(30),
  last_name  VARCHAR(30),
  address    VARCHAR(255),
  city       VARCHAR(80),
  telephone  VARCHAR(20),
  clinic_id  VARCHAR(36)
);
CREATE INDEX owners_last_name ON owners (last_name);
CREATE INDEX owners_clinic_id ON owners (clinic_id);
ALTER TABLE owners ADD CONSTRAINT fk_owners_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id);

CREATE TABLE pets (
  id         VARCHAR(36) PRIMARY KEY,
  name       VARCHAR(30),
  birth_date DATE,
  type_id    INTEGER NOT NULL,
  owner_id   VARCHAR(36) NOT NULL,
  gender     VARCHAR(10),
  vaccination_status VARCHAR(20),
  medical_notes VARCHAR(1000)
);
ALTER TABLE pets ADD CONSTRAINT fk_pets_owners FOREIGN KEY (owner_id) REFERENCES owners (id);
ALTER TABLE pets ADD CONSTRAINT fk_pets_types FOREIGN KEY (type_id) REFERENCES types (id);
CREATE INDEX pets_name ON pets (name);

CREATE TABLE clinic_users (
  id                VARCHAR(36) PRIMARY KEY,
  clinic_id         VARCHAR(36) NOT NULL,
  email             VARCHAR(120) NOT NULL,
  password_hash     VARCHAR(120) NOT NULL,
  display_name      VARCHAR(80) NOT NULL,
  veterinarian_id   VARCHAR(36),
  clinic_admin      BOOLEAN DEFAULT FALSE NOT NULL
);
ALTER TABLE clinic_users ADD CONSTRAINT fk_clinic_users_clinic FOREIGN KEY (clinic_id) REFERENCES clinics (id);
CREATE UNIQUE INDEX clinic_users_email ON clinic_users (email);
