CREATE DATABASE IF NOT EXISTS petclinic;

USE petclinic;

CREATE TABLE IF NOT EXISTS types (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80),
  INDEX(name)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS clinics (
  id CHAR(36) NOT NULL PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  phone VARCHAR(30),
  address VARCHAR(255),
  INDEX(name)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS owners (
  id CHAR(36) NOT NULL PRIMARY KEY,
  first_name VARCHAR(30),
  last_name VARCHAR(30),
  address VARCHAR(255),
  city VARCHAR(80),
  telephone VARCHAR(20),
  clinic_id CHAR(36) NULL,
  INDEX(last_name),
  INDEX idx_owners_clinic (clinic_id),
  CONSTRAINT fk_owners_clinic FOREIGN KEY (clinic_id) REFERENCES clinics(id)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS pets (
  id CHAR(36) NOT NULL PRIMARY KEY,
  name VARCHAR(30),
  birth_date DATE,
  type_id INT(4) UNSIGNED NOT NULL,
  owner_id CHAR(36) NOT NULL,
  gender VARCHAR(10),
  vaccination_status VARCHAR(20),
  medical_notes VARCHAR(1000),
  INDEX(name),
  FOREIGN KEY (owner_id) REFERENCES owners(id),
  FOREIGN KEY (type_id) REFERENCES types(id)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS clinic_users (
  id CHAR(36) NOT NULL PRIMARY KEY,
  clinic_id CHAR(36) NOT NULL,
  email VARCHAR(120) NOT NULL,
  password_hash VARCHAR(120) NOT NULL,
  display_name VARCHAR(80) NOT NULL,
  veterinarian_id CHAR(36) NULL,
  clinic_admin TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_clinic_users_email (email),
  FOREIGN KEY (clinic_id) REFERENCES clinics(id)
) engine=InnoDB;
