CREATE TABLE ssn_audit_entries (
  id                        BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  cvr                       VARCHAR(8) NOT NULL,
  device_id                 VARCHAR(36) NOT NULL,
  user_name                 VARCHAR(255) NOT NULL,
  user_uuid                 VARCHAR(36) NOT NULL,
  ts                        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);