CREATE TABLE sent_emails (
  id CHAR(36) NOT NULL PRIMARY KEY,
  `to` VARCHAR(255) NOT NULL,
  to_name VARCHAR(255),
  subject VARCHAR(150) NOT NULL,
  body TEXT NOT NULL,
  status VARCHAR(20) NOT NULL,
  sent_at TIMESTAMP NOT NULL,
  provider_message_id VARCHAR(255),
  error_message VARCHAR(2000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;