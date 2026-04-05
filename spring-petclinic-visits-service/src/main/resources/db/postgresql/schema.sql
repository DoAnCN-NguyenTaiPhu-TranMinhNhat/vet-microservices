-- PostgreSQL schema for visits (pet_id UUID — aligns with customers-service pets.id)

CREATE TABLE IF NOT EXISTS visits (
  id SERIAL PRIMARY KEY,
  pet_id UUID NOT NULL,
  visit_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  description VARCHAR(8192),
  temperature NUMERIC(4,1) CHECK (temperature >= 35.0 AND temperature <= 43.0),
  weight_kg NUMERIC(5,2) CHECK (weight_kg >= 0.1 AND weight_kg <= 100.0),
  symptoms_list VARCHAR(5000),
  heart_rate INTEGER CHECK (heart_rate >= 40),
  symptom_duration INTEGER CHECK (symptom_duration >= 0),
  target_diagnosis VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_visits_pet_id ON visits(pet_id);
CREATE INDEX IF NOT EXISTS idx_visits_visit_date ON visits(visit_date);
CREATE INDEX IF NOT EXISTS idx_visits_target_diagnosis ON visits(target_diagnosis);
