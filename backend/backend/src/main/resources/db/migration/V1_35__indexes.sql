create index idx_servers_key on servers (api_key);
create index idx_notifications_polling_key on notifications (polling_key);
create index idx_clients_locked on clients (locked);
create index idx_clients_yubikey on clients (yubikey_uid);
