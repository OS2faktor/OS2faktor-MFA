CREATE TABLE statistics_result (
  id                        BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  tts                       DATE,
  logins                    BIGINT NOT NULL,
  server_id                 BIGINT NOT NULL,
  cvr                       VARCHAR(8) NOT NULL,
  municipality_name         VARCHAR(255) NOT NULL,
  server_name               VARCHAR(255) NOT NULL
);

CREATE TABLE statistics_result_current (
  id                        BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  logins                    BIGINT NOT NULL,
  server_id                 BIGINT NOT NULL,
  cvr                       VARCHAR(8) NOT NULL,
  municipality_name         VARCHAR(255) NOT NULL,
  server_name               VARCHAR(255) NOT NULL
);