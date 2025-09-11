drop index idx_notifications_client_type on notifications;
create index idx_notifications_composite on notifications (client_type, created);
