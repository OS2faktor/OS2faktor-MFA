ALTER TABLE clients ADD COLUMN associated_user_timestamp TIMESTAMP NULL AFTER user_id;

UPDATE clients SET associated_user_timestamp=created WHERE user_id IS NOT NULL;