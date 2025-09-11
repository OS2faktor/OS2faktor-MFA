alter table notifications add column client_type VARCHAR(64) NOT NULL DEFAULT '????';

create index idx_notifications_client_type on notifications (client_type);
