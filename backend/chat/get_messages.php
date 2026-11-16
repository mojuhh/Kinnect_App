<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$peerId = (int)($_GET['peer_id'] ?? 0);
$afterId = (int)($_GET['after_id'] ?? 0);
$since = $_GET['since'] ?? null;

if (!$peerId) {
    fail('peer_id required');
}

$conn = db();

if ($since) {
    $st = $conn->prepare('
        SELECT * FROM messages
        WHERE ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?))
          AND updated_at > ?
        ORDER BY id ASC
    ');
    $st->execute([$uid, $peerId, $peerId, $uid, $since]);
} else {
    $st = $conn->prepare('
        SELECT * FROM messages
        WHERE ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?))
          AND id > ?
        ORDER BY id ASC
    ');
    $st->execute([$uid, $peerId, $peerId, $uid, $afterId]);
}

$messages = $st->fetchAll();
out($messages);
?>
