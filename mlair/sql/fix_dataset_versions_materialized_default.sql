-- ml-air alembic 0018 adds dataset_versions.materialized_from_buffer NOT NULL, then removes
-- server_default. Inserts from create_dataset_version_from_csv_upload omit the column → NULL
-- → 500 with body "Internal Server Error" (text/plain) → browser JSON.parse error on upload.
--
-- vet-microservices: this is applied automatically after `alembic upgrade head` in docker-compose
-- (mlair-api command). Use this file only for a manual fix (external Postgres, or debugging).

ALTER TABLE dataset_versions
  ALTER COLUMN materialized_from_buffer SET DEFAULT false;

UPDATE dataset_versions
SET materialized_from_buffer = false
WHERE materialized_from_buffer IS NULL;
