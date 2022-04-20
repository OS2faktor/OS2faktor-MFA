CREATE TABLE external_login_session (
  id                          BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  tts                         TIMESTAMP NOT NULL,
  ssn                         VARCHAR(128) NOT NULL,
  nsis_level                  VARCHAR(64) NOT NULL,
  login_service_provider_id   BIGINT NOT NULL,
  session_key                 VARCHAR(36) NOT NULL,

  FOREIGN KEY (login_service_provider_id) REFERENCES login_service_provider(id) ON DELETE CASCADE
);