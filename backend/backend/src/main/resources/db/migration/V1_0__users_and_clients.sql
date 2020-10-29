CREATE TABLE users (
  id                        BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  ssn                       VARCHAR(128) NOT NULL,
  pid                       VARCHAR(255) NOT NULL
);

CREATE TABLE clients (
  device_id                 VARCHAR(36) NOT NULL PRIMARY KEY,
  created                   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  api_key                   VARCHAR(36) NOT NULL,
  client_type               VARCHAR(64) NOT NULL,
  name                      VARCHAR(255) NOT NULL,
  notification_key          VARCHAR(255),
  token                     TEXT,
  user_id                   BIGINT,
  client_version            VARCHAR(32),
  pincode                   VARCHAR(6),
  locked                    BOOLEAN NOT NULL DEFAULT 0,
  locked_until              TIMESTAMP NULL,
  use_count                 BIGINT NOT NULL DEFAULT 0,
  last_used                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  disabled                  BOOLEAN NOT NULL DEFAULT 0,
  
  INDEX(notification_key),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
