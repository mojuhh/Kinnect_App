-- Kinnect Application Database Schema
CREATE DATABASE IF NOT EXISTS kinnect_db;
USE kinnect_db;

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(128) UNIQUE NOT NULL,
    email VARCHAR(255) NOT NULL,
    username VARCHAR(100),
    name VARCHAR(100),
    bio TEXT,
    phone VARCHAR(30),
    status VARCHAR(20) DEFAULT 'offline',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Posts Table
CREATE TABLE IF NOT EXISTS posts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id VARCHAR(128) UNIQUE NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    username VARCHAR(100),
    content TEXT,
    image_url TEXT,
    timestamp BIGINT NOT NULL,
    likes_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Stories Table
CREATE TABLE IF NOT EXISTS stories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    story_id VARCHAR(128) UNIQUE NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    username VARCHAR(100),
    user_avatar VARCHAR(50),
    media_uri TEXT,
    media_type VARCHAR(20) DEFAULT 'image',
    text_label TEXT,
    audience VARCHAR(50) DEFAULT 'your_story',
    timestamp BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Messages Table
CREATE TABLE IF NOT EXISTS messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(128) UNIQUE NOT NULL,
    chat_id VARCHAR(255) NOT NULL,
    sender_id VARCHAR(128) NOT NULL,
    sender_name VARCHAR(100),
    type VARCHAR(20) DEFAULT 'text',
    content TEXT,
    image_url LONGTEXT,
    reply_to_id VARCHAR(128),
    reply_to_text TEXT,
    is_edited TINYINT(1) DEFAULT 0,
    is_deleted TINYINT(1) DEFAULT 0,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Chats Metadata Table
CREATE TABLE IF NOT EXISTS chats (
    chat_id VARCHAR(255) PRIMARY KEY,
    last_message TEXT,
    last_message_time BIGINT,
    last_sender_id VARCHAR(128),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Friends Table
CREATE TABLE IF NOT EXISTS friends (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    friend_id VARCHAR(128) NOT NULL,
    status VARCHAR(20) DEFAULT 'accepted',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    title VARCHAR(255),
    message TEXT,
    is_read TINYINT(1) DEFAULT 0,
    timestamp BIGINT NOT NULL
);
