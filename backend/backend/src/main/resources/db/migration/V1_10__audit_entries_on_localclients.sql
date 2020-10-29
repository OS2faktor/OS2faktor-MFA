DROP TABLE ssn_audit_entries;

ALTER TABLE local_clients ADD COLUMN admin_user_name VARCHAR(255);
ALTER TABLE local_clients ADD COLUMN admin_user_uuid VARCHAR(36);
ALTER TABLE local_clients ADD COLUMN ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP;