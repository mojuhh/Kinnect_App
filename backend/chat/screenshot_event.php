<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$peerId = (int)($in['peer_id'] ?? 0);
$uuid = $in['client_uuid'] ?? fail('client_uuid required');

if (!$peerId) {
    fail('peer_id required');
}

$conn = db();

$stUser = $conn->prepare('SELECT name FROM users WHERE id=?');
$stUser->execute([$uid]);
$userName = $stUser->fetch()['name'] ?? 'Someone';

$content = "$userName took a screenshot";

$st = $conn->prepare('INSERT INTO messages (sender_id, receiver_id, type, content, client_uuid) VALUES (?, ?, "system", ?, ?)');
$st->execute([$uid, $peerId, $content, $uuid]);

$stN = $conn->prepare('INSERT INTO notifications (user_id, type, from_user_id, data) VALUES (?, "screenshot", ?, ?)');
$stN->execute([$peerId, $uid, json_encode(['text' => $content])]);

out(['message' => 'Screenshot event logged', 'content' => $content]);
?>
