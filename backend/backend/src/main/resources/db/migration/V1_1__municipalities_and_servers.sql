CREATE TABLE municipalities (
  id                        BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  cvr                       VARCHAR(8) NOT NULL,
  api_key                   VARCHAR(36) NOT NULL,
  name                      VARCHAR(255) NOT NULL
);

CREATE TABLE pseudonyms (
  id                        BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  cvr                       VARCHAR(8) NOT NULL,
  pseudonym                 VARCHAR(128) NOT NULL,
  ssn                       VARCHAR(60) NOT NULL,
  
  INDEX(cvr, pseudonym)
);

CREATE TABLE servers (
  id                        BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  name                      VARCHAR(255) NOT NULL,
  api_key                   VARCHAR(36) NOT NULL,
  use_count                 BIGINT NOT NULL DEFAULT 0,
  municipality_id           BIGINT NOT NULL,
  
  FOREIGN KEY (municipality_id) REFERENCES municipalities(id) ON DELETE CASCADE
);