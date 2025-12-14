ALTER TABLE notifications ADD system_name VARCHAR(255) NULL;
ALTER TABLE notifications ADD username varchar(255) NULL;

ALTER TABLE notifications_history ADD system_name varchar(255) NULL;
ALTER TABLE notifications_history ADD username varchar(255) NULL;