<?php
require_once __DIR__ . '/../common.php';

$in = body();
$name = trim($in['name'] ?? '');
$email = trim($in['email'] ?? '');
$phone = trim($in['phone'] ?? '');
$password = $in['password'] ?? '';

if (empty($name) || empty($email) || empty($password)) {
    fail('Name, email and password are required');
}

if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    fail('Enter a valid email address');
}

if ($phone !== '') {
    $phone = preg_replace('/[\s().-]+/', '', $phone);
    if (!preg_match('/^\+[1-9][0-9]{7,14}$/', $phone)) {
        fail('Phone number must use international format, e.g. +15551234567');
    }
}

if (strlen($password) < 8) {
    fail('Password must be at least 8 characters');
}

$conn = db();
if ($phone === '') {
    $st = $conn->prepare('SELECT id FROM users WHERE email=?');
    $st->execute([$email]);
} else {
    $st = $conn->prepare('SELECT id FROM users WHERE email=? OR phone_number=?');
    $st->execute([$email, $phone]);
}
if ($st->fetch()) {
    fail('Email or phone number is already registered', 409);
}

$hash = password_hash($password, PASSWORD_DEFAULT);
$st = $conn->prepare('INSERT INTO users (name, email, phone_number, password_hash, profile_complete) VALUES (?, ?, ?, ?, 0)');
try {
    $st->execute([$name, $email, $phone !== '' ? $phone : null, $hash]);
} catch (PDOException $e) {
    if ($e->getCode() === '23000') {
        fail('Email or phone number is already registered', 409);
    }
    throw $e;
}
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
