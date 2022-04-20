CREATE TABLE login_service_provider (
  id                        BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name                      VARCHAR(255) NOT NULL,
  cvr                       VARCHAR(8) NOT NULL,
  api_key                   VARCHAR(36) NOT NULL
);