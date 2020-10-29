ALTER TABLE clients ADD COLUMN yubikey_uid VARCHAR(255);
ALTER TABLE clients ADD COLUMN yubikey_attestation TEXT;