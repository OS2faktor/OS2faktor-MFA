ALTER TABLE notifications_history DROP INDEX idx_nh_created_id;
ALTER TABLE notifications_history ADD INDEX idx_nh_server_created (server_id, created);
