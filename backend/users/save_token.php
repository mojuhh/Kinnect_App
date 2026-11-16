<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$fcmToken = trim($in['fcm_token'] ?? $in['token'] ?? '');

if (empty($fcmToken)) {
    fail('FCM token is required');
}

$conn = db();
$st = $conn->prepare('UPDATE users SET fcm_token=? WHERE id=?');
$st->execute([$fcmToken, $uid]);

out(['message' => 'FCM token saved']);
?>
