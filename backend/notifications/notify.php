<?php
require_once __DIR__ . '/../common.php';

function send_fcm_push($targetUserId, $title, $body, $data = []) {
    $conn = db();
    $st = $conn->prepare('SELECT fcm_token FROM users WHERE id=?');
    $st->execute([$targetUserId]);
    $token = $st->fetch()['fcm_token'] ?? null;

    if (!$token) {
        return false;
    }

    // FCM HTTP v1 implementation stub
    return true;
}
?>
