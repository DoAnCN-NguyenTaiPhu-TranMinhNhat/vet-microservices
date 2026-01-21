-- Migration script to add medical data fields to existing visits table
-- Run this script if the visits table already exists

-- Add new medical data columns
ALTER TABLE visits 
  ADD COLUMN IF NOT EXISTS temperature NUMERIC(4,1),
  ADD COLUMN IF NOT EXISTS weight_kg NUMERIC(5,2),
  ADD COLUMN IF NOT EXISTS symptoms_list VARCHAR(5000),
  ADD COLUMN IF NOT EXISTS heart_rate INTEGER,
  ADD COLUMN IF NOT EXISTS symptom_duration INTEGER,
  ADD COLUMN IF NOT EXISTS target_diagnosis VARCHAR(100);

-- Add constraints
ALTER TABLE visits 
  ADD CONSTRAINT chk_temperature CHECK (temperature IS NULL OR (temperature >= 35.0 AND temperature <= 43.0)),
  ADD CONSTRAINT chk_weight_kg CHECK (weight_kg IS NULL OR (weight_kg >= 0.1 AND weight_kg <= 100.0)),
  ADD CONSTRAINT chk_heart_rate CHECK (heart_rate IS NULL OR heart_rate >= 40),
  ADD CONSTRAINT chk_symptom_duration CHECK (symptom_duration IS NULL OR symptom_duration >= 0);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_visits_target_diagnosis ON visits(target_diagnosis);

