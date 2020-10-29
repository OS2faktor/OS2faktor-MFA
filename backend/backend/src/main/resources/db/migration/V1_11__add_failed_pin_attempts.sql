ALTER TABLE clients ADD COLUMN failed_pin_attempts BIGINT;
UPDATE clients SET failed_pin_attempts = 0;
ALTER TABLE clients MODIFY COLUMN failed_pin_attempts BIGINT NOT NULL DEFAULT 0;