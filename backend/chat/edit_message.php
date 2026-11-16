<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$messageId = (int)($in['message_id'] ?? 0);
$newText = trim($in['content'] ?? $in['text'] ?? '');

if (!$messageId || empty($newText)) {
    fail('message_id and content are required');
}

$conn = db();
$st = $conn->prepare('
    SELECT id, TIMESTAMPDIFF(SECOND, created_at, UTC_TIMESTAMP()) as age_sec
    FROM messages
    WHERE id = ? AND sender_id = ? AND is_deleted = 0
');
$st->execute([$messageId, $uid]);
$msg = $st->fetch();

if (!$msg) {
    fail('Message not found or unauthorized', 404);
}

if ($msg['age_sec'] > 300) { // 5 minutes = 300 seconds
    fail('Edit window expired (5 minute limit)', 403);
}

$stUp = $conn->prepare('UPDATE messages SET content = ?, edited_at = UTC_TIMESTAMP() WHERE id = ?');
$stUp->execute([$newText, $messageId]);

$stFetch = $conn->prepare('SELECT * FROM messages WHERE id = ?');
$stFetch->execute([$messageId]);
out($stFetch->fetch());
?>
