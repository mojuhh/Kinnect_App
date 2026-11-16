<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$targetId = (int)($in['user_id'] ?? $in['receiver_id'] ?? 0);
$uuid = $in['client_uuid'] ?? '';

if (!$targetId || $targetId == $uid) {
    fail('Invalid target user');
}

$conn = db();

if ($uuid) {
    $st = $conn->prepare('SELECT * FROM friendships WHERE client_uuid=?');
    $st->execute([$uuid]);
    if ($row = $st->fetch()) out($row);
}

try {
    $st = $conn->prepare('INSERT INTO friendships (sender_id, receiver_id, status, client_uuid) VALUES (?, ?, "pending", ?)');
    $st->execute([$uid, $targetId, $uuid]);
} catch (PDOException $e) {
    if ($e->getCode() == 23000) {
        // If pair already exists
        $st = $conn->prepare('SELECT * FROM friendships WHERE (sender_id=? AND receiver_id=?) OR (sender_id=? AND receiver_id=?)');
        $st->execute([$uid, $targetId, $targetId, $uid]);
        if ($row = $st->fetch()) out($row);
    } else {
        throw $e;
    }
}

// Notification
$stN = $conn->prepare('INSERT INTO notifications (user_id, type, from_user_id, data) VALUES (?, "friend_request", ?, ?)');
$stN->execute([$targetId, $uid, json_encode(['from_id' => $uid])]);

out(['message' => 'Friend request sent']);
?>
