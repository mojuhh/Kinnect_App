<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$senderId = (int)($in['user_id'] ?? $in['sender_id'] ?? 0);

if (!$senderId) {
    fail('User ID required');
}

$conn = db();
$st = $conn->prepare('UPDATE friendships SET status="accepted" WHERE (sender_id=? AND receiver_id=?) OR (sender_id=? AND receiver_id=?)');
$st->execute([$senderId, $uid, $uid, $senderId]);

out(['message' => 'Friend request accepted']);
?>
