CREATE DATABASE IF NOT EXISTS kinnect DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kinnect;

CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  username VARCHAR(40) UNIQUE NULL,
  email VARCHAR(120) UNIQUE NOT NULL,
  phone_number VARCHAR(16) UNIQUE NULL,
  password_hash VARCHAR(255) NOT NULL,
  bio VARCHAR(255) DEFAULT '',
  profile_pic VARCHAR(255) NULL,
  cover_pic VARCHAR(255) NULL,
  profile_complete TINYINT DEFAULT 0,
  is_online TINYINT DEFAULT 0,
  last_seen DATETIME NULL,
  fcm_token VARCHAR(255) NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sessions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  token CHAR(64) UNIQUE NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS friendships (
  id INT AUTO_INCREMENT PRIMARY KEY,
  sender_id INT NOT NULL,
  receiver_id INT NOT NULL,
  status ENUM('pending', 'accepted') DEFAULT 'pending',
  client_uuid CHAR(36) UNIQUE NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY pair (sender_id, receiver_id)
);

CREATE TABLE IF NOT EXISTS posts (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  text TEXT,
  client_uuid CHAR(36) UNIQUE NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS post_images (
  id INT AUTO_INCREMENT PRIMARY KEY,
  post_id INT NOT NULL,
  path VARCHAR(255) NOT NULL,
  sort_order INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS reactions (
  post_id INT NOT NULL,
  user_id INT NOT NULL,
  type ENUM('like', 'love', 'care', 'haha', 'wow', 'sad') NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (post_id, user_id)
);

CREATE TABLE IF NOT EXISTS comments (
  id INT AUTO_INCREMENT PRIMARY KEY,
  post_id INT NOT NULL,
  user_id INT NOT NULL,
  parent_id INT NULL,
  text TEXT,
  image_path VARCHAR(255) NULL,
  client_uuid CHAR(36) UNIQUE NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS comment_likes (
  comment_id INT NOT NULL,
  user_id INT NOT NULL,
  PRIMARY KEY (comment_id, user_id)
);

CREATE TABLE IF NOT EXISTS stories (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  media_path VARCHAR(255) NOT NULL,
  media_type ENUM('image', 'video') NOT NULL,
  label VARCHAR(120),
  audience ENUM('story', 'close_friends') DEFAULT 'story',
  client_uuid CHAR(36) UNIQUE NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS close_friends (
  user_id INT NOT NULL,
  friend_id INT NOT NULL,
  PRIMARY KEY (user_id, friend_id)
);

CREATE TABLE IF NOT EXISTS messages (
  id INT AUTO_INCREMENT PRIMARY KEY,
  sender_id INT NOT NULL,
  receiver_id INT NOT NULL,
  type ENUM('text', 'image', 'video', 'file', 'system') DEFAULT 'text',
  content TEXT,
  media_path VARCHAR(255) NULL,
  file_name VARCHAR(150) NULL,
  client_uuid CHAR(36) UNIQUE NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  edited_at DATETIME NULL,
  is_deleted TINYINT DEFAULT 0,
  INDEX conv (sender_id, receiver_id, id)
);

CREATE TABLE IF NOT EXISTS calls (
  id INT AUTO_INCREMENT PRIMARY KEY,
  caller_id INT NOT NULL,
  receiver_id INT NOT NULL,
  type ENUM('audio', 'video'),
  status ENUM('missed', 'answered', 'declined'),
  started_at DATETIME,
  duration INT DEFAULT 0,
  client_uuid CHAR(36) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS notifications (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  type VARCHAR(30),
  from_user_id INT,
  data TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
