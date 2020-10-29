ALTER TABLE notifications ADD COLUMN server_id BIGINT;
ALTER TABLE notifications ADD COLUMN redirect_url VARCHAR(255);
ALTER TABLE notifications MODIFY COLUMN challenge VARCHAR(255);