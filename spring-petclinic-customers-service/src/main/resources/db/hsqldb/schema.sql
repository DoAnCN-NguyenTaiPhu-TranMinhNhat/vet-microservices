DROP TABLE pets IF EXISTS;
DROP TABLE types IF EXISTS;
DROP TABLE owners IF EXISTS;
DROP SEQUENCE owner_sequence IF EXISTS;
DROP SEQUENCE pet_sequence IF EXISTS;

-- Create sequences for ID generation starting from 1
-- Hibernate uses "call next value for <sequence>" with @SequenceGenerator on HSQLDB
CREATE SEQUENCE owner_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE pet_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE types (
  id   INTEGER IDENTITY PRIMARY KEY,
  name VARCHAR(80)
);
CREATE INDEX types_name ON types (name);

CREATE TABLE owners (
  id         INTEGER IDENTITY PRIMARY KEY,
  first_name VARCHAR(30),
  last_name  VARCHAR(30),
  address    VARCHAR(255),
  city       VARCHAR(80),
  telephone  VARCHAR(20)
);
CREATE INDEX owners_last_name ON owners (last_name);

CREATE TABLE pets (
  id         INTEGER IDENTITY PRIMARY KEY,
  name       VARCHAR(30),
  birth_date DATE,
  type_id    INTEGER NOT NULL,
  owner_id   INTEGER NOT NULL,
  gender     VARCHAR(10),
  vaccination_status VARCHAR(20),
  medical_notes VARCHAR(1000)
);
ALTER TABLE pets ADD CONSTRAINT fk_pets_owners FOREIGN KEY (owner_id) REFERENCES owners (id);
ALTER TABLE pets ADD CONSTRAINT fk_pets_types FOREIGN KEY (type_id) REFERENCES types (id);
CREATE INDEX pets_name ON pets (name);
