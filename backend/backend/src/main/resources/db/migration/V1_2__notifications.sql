CREATE TABLE notifications (
  subscription_key                   VARCHAR(36) NOT NULL PRIMARY KEY,
  polling_key                        VARCHAR(36) NOT NULL,
  created                            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  server_name                        VARCHAR(255) NOT NULL,
  client_notified                    BOOLEAN NOT NULL,
  client_authenticated               BOOLEAN NOT NULL,
  client_rejected                    BOOLEAN NOT NULL,
  client_device_id                   VARCHAR(36) NOT NULL,
  challenge                          VARCHAR(4) NOT NULL,

  FOREIGN KEY (client_device_id) REFERENCES clients(device_id) ON DELETE CASCADE
);