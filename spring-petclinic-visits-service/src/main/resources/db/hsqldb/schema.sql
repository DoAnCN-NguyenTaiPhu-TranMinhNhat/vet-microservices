DROP TABLE visits IF EXISTS;

CREATE TABLE visits (
  id          INTEGER IDENTITY PRIMARY KEY,
  pet_id      INTEGER NOT NULL,
  visit_date  DATE,
  description VARCHAR(8192),
  -- Medical data fields
  temperature NUMERIC(4,1) CHECK (temperature >= 35.0 AND temperature <= 43.0),
  weight_kg NUMERIC(5,2) CHECK (weight_kg >= 0.1 AND weight_kg <= 100.0),
  symptoms_list VARCHAR(5000),
  heart_rate INTEGER CHECK (heart_rate >= 40),
  symptom_duration INTEGER CHECK (symptom_duration >= 0),
  target_diagnosis VARCHAR(100)
);

CREATE INDEX visits_pet_id ON visits (pet_id);
CREATE INDEX visits_visit_date ON visits (visit_date);
CREATE INDEX visits_target_diagnosis ON visits (target_diagnosis);
