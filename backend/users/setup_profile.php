<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$username = strtolower(trim($in['username'] ?? ''));
$bio = trim($in['bio'] ?? '');

if (empty($username)) {
    fail('Username is required');
}

$conn = db();
$st = $conn->prepare('SELECT id FROM users WHERE username=? AND id!=?');
$st->execute([$username, $uid]);
if ($st->fetch()) {
    fail('Username is already taken');
}

$st = $conn->prepare('UPDATE users SET username=?, bio=?, profile_complete=1 WHERE id=?');
$st->execute([$username, $bio, $uid]);

out(['message' => 'Profile setup complete']);
?>
