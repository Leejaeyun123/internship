CREATE DATABASE IF NOT EXISTS chat_app;
USE chat_app;

CREATE TABLE IF NOT EXISTS users (
    id        VARCHAR(50) PRIMARY KEY,
    password  VARCHAR(255) NOT NULL,       
    nickname  VARCHAR(50) NOT NULL,        
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_logs (
    id        VARCHAR(50) NOT NULL,                  
    nickname  VARCHAR(50) NOT NULL,                   
    message   TEXT NOT NULL,                          
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,     
    PRIMARY KEY (id, timestamp)
);

