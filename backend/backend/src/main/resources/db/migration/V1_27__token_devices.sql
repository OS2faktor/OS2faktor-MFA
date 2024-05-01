CREATE TABLE totph_devices (
  id                                 BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  serialnumber                       VARCHAR(255) NOT NULL,
  secret_key                         VARCHAR(255) NOT NULL,
  registered                         BOOLEAN NOT NULL DEFAULT FALSE,
  registered_to_cpr                  VARCHAR(255) NULL,
  registered_to_cvr                  VARCHAR(8) NULL,
  client_device_id                   VARCHAR(36) NULL
);