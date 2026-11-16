<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$targetId = (int)($in['user_id'] ?? $in['target_id'] ?? 0);

if (!$targetId) {
    fail('User ID required');
}

$conn = db();
$st = $conn->prepare('DELETE FROM friendships WHERE (sender_id=? AND receiver_id=?) OR (sender_id=? AND receiver_id=?)');
$st->execute([$uid, $targetId, $targetId, $uid]);

out(['message' => 'Friend request declined/removed']);
?>
