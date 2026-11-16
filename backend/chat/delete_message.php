<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$messageId = (int)($in['message_id'] ?? 0);

if (!$messageId) {
    fail('message_id required');
}

$conn = db();
$st = $conn->prepare('
    SELECT id, TIMESTAMPDIFF(SECOND, created_at, UTC_TIMESTAMP()) as age_sec
    FROM messages
    WHERE id = ? AND sender_id = ?
');
$st->execute([$messageId, $uid]);
$msg = $st->fetch();

if (!$msg) {
    fail('Message not found or unauthorized', 404);
}

if ($msg['age_sec'] > 300) { // 5 minutes = 300 seconds
    fail('Delete window expired (5 minute limit)', 403);
}

$stUp = $conn->prepare('UPDATE messages SET is_deleted = 1, content = "This message was deleted", media_path = NULL WHERE id = ?');
$stUp->execute([$messageId]);

$stFetch = $conn->prepare('SELECT * FROM messages WHERE id = ?');
$stFetch->execute([$messageId]);
out($stFetch->fetch());
?>
