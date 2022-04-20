ALTER TABLE notifications ADD COLUMN client_fetched_timestamp           TIMESTAMP NULL AFTER client_device_id;
ALTER TABLE notifications ADD COLUMN client_response_timestamp          TIMESTAMP NULL AFTER client_fetched_timestamp;
ALTER TABLE notifications ADD COLUMN sent_timestamp                     TIMESTAMP NULL AFTER created;

CREATE TABLE notifications_history (
  subscription_key                   VARCHAR(36) NOT NULL PRIMARY KEY,
  polling_key                        VARCHAR(36) NULL,
  created                            TIMESTAMP NULL,
  sent_timestamp                     TIMESTAMP NULL,
  server_name                        VARCHAR(255) NULL,
  client_notified                    BOOLEAN NULL,
  client_authenticated               BOOLEAN NULL,
  client_rejected                    BOOLEAN NULL,
  client_device_id                   VARCHAR(36) NULL,
  client_fetched_timestamp           TIMESTAMP NULL,
  client_response_timestamp          TIMESTAMP NULL,
  challenge                          VARCHAR(4) NULL,
  server_id                          BIGINT NULL,
  redirect_url                       VARCHAR(255) NULL
);