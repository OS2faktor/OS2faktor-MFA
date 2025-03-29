ALTER TABLE clients ADD COLUMN passwordless BOOLEAN DEFAULT FALSE;
ALTER TABLE notifications ADD COLUMN passwordless BOOLEAN DEFAULT FALSE;
ALTER TABLE notifications ADD COLUMN passwordless_challenge VARCHAR(16) NULL;
ALTER TABLE notifications_history ADD COLUMN passwordless BOOLEAN DEFAULT FALSE;
ALTER TABLE notifications_history ADD COLUMN passwordless_challenge VARCHAR(16) NULL;
