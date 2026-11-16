<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$friendId = (int)($in['user_id'] ?? $in['friend_id'] ?? 0);

if (!$friendId) {
    fail('Friend ID required');
}

$conn = db();
$st = $conn->prepare('DELETE FROM friendships WHERE (sender_id=? AND receiver_id=?) OR (sender_id=? AND receiver_id=?)');
$st->execute([$uid, $friendId, $friendId, $uid]);

out(['message' => 'Friend removed']);
?>
