<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$h = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '';
preg_match('/Bearer\s+([a-f0-9]{64})/', $h, $m);
$token = $m[1] ?? '';

$conn = db();
if ($token) {
    $st = $conn->prepare('DELETE FROM sessions WHERE token=?');
    $st->execute([$token]);
}

$st = $conn->prepare('UPDATE users SET is_online=0, fcm_token=NULL WHERE id=?');
$st->execute([$uid]);

out(['message' => 'Logged out successfully']);
?>
