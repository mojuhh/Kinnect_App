<?php
require_once __DIR__ . '/../common.php';

$in = body();
$email = trim($in['email'] ?? '');
$password = $in['password'] ?? '';

if (empty($email) || empty($password)) {
    fail('Email and password are required');
}

$conn = db();
$st = $conn->prepare('SELECT id, password_hash, profile_complete, name, username FROM users WHERE email=?');
$st->execute([$email]);
$user = $st->fetch();

if (!$user || !password_verify($password, $user['password_hash'])) {
    fail('Invalid email or password', 400);
}

$token = bin2hex(random_bytes(32));
$st = $conn->prepare('INSERT INTO sessions (user_id, token) VALUES (?, ?)');
$st->execute([$user['id'], $token]);

$st = $conn->prepare('UPDATE users SET is_online=1, last_seen=UTC_TIMESTAMP() WHERE id=?');
$st->execute([$user['id']]);

out([
    'token' => $token,
    'user_id' => (int)$user['id'],
    'name' => $user['name'],
    'username' => $user['username'],
    'profile_complete' => (int)$user['profile_complete']
]);
?>
