CREATE TABLE statistics (
  id                        BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  server_id                 BIGINT NOT NULL,
  cvr                       VARCHAR(8) NOT NULL,
  device_id                 VARCHAR(36) NOT NULL,
  client_type               VARCHAR(64) NOT NULL,
  client_version            VARCHAR(32),
  created                   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);