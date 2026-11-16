<?php
require_once __DIR__ . '/../common.php';

$in = body();
$name = trim($in['name'] ?? '');
$email = trim($in['email'] ?? '');
$password = $in['password'] ?? '';

if (empty($name) || empty($email) || empty($password)) {
    fail('Name, email and password are required');
}

if (strlen($password) < 8) {
    fail('Password must be at least 8 characters');
}

$conn = db();
$st = $conn->prepare('SELECT id FROM users WHERE email=?');
$st->execute([$email]);
if ($st->fetch()) {
    fail('Email is already registered');
}

$hash = password_hash($password, PASSWORD_DEFAULT);
$st = $conn->prepare('INSERT INTO users (name, email, password_hash, profile_complete) VALUES (?, ?, ?, 0)');
$st->execute([$name, $email, $hash]);
$userId = (int)$conn->lastInsertId();

$token = bin2hex(random_bytes(32));
$st = $conn->prepare('INSERT INTO sessions (user_id, token) VALUES (?, ?)');
$st->execute([$userId, $token]);

out([
    'token' => $token,
    'user_id' => $userId,
    'profile_complete' => 0
]);
?>
