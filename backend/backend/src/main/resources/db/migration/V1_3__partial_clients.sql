CREATE TABLE partial_clients (
  id                        BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,

  created                   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  client_type               VARCHAR(64) NOT NULL,
  name                      VARCHAR(255) NOT NULL,
  user_id                   BIGINT,
  challenge                 VARCHAR(255) NOT NULL,
  
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
