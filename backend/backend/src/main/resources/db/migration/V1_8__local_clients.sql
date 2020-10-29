CREATE TABLE local_clients (
  id                        BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  device_id                 VARCHAR(36) NOT NULL,
  cvr                       VARCHAR(8) NOT NULL,
  ssn                       VARCHAR(128) NOT NULL,

  INDEX(cvr, ssn),
  INDEX(cvr, device_id),
  FOREIGN KEY (device_id) REFERENCES clients(device_id) ON DELETE CASCADE
);

ALTER TABLE users ADD INDEX(ssn);
