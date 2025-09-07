CREATE DATABASE IF NOT EXISTS chat_app
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE chat_app;

CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(50)  PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    nickname      VARCHAR(50)  NOT NULL UNIQUE,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS chat_logs (
    id        VARCHAR(50)  NOT NULL,
    nickname  VARCHAR(50)  NOT NULL,
    message   TEXT         NOT NULL,
    room      VARCHAR(50)  NOT NULL DEFAULT 'Lobby',
    timestamp DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, timestamp),
    KEY idx_logs_room_time (room, timestamp),
    KEY idx_logs_user_time (id, timestamp),
    CONSTRAINT fk_logs_user
        FOREIGN KEY (id) REFERENCES users(id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
