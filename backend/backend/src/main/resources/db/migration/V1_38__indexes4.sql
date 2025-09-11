alter table notifications_history add column client_type varchar(64);
create index idx_nh_created_id on notifications_history (created, server_id);
