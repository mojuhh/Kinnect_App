<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$receiverId = (int)($in['receiver_id'] ?? $in['peer_id'] ?? 0);
$type = $in['type'] ?? 'text'; // text | image | video | file | system
$content = trim($in['content'] ?? '');
$uuid = $in['client_uuid'] ?? fail('client_uuid required');

if (!$receiverId) {
    fail('receiver_id required');
}

$conn = db();

$st = $conn->prepare('SELECT * FROM messages WHERE client_uuid=?');
$st->execute([$uuid]);
if ($row = $st->fetch()) {
    out($row);
}

$mediaPath = save_upload('file', 'chat', ['jpg', 'jpeg', 'png', 'mp4', 'pdf', 'doc', 'docx']);
$fileName = !empty($_FILES['file']['name']) ? $_FILES['file']['name'] : null;

$st = $conn->prepare('
    INSERT INTO messages (sender_id, receiver_id, type, content, media_path, file_name, client_uuid)
    VALUES (?, ?, ?, ?, ?, ?, ?)
');
$st->execute([$uid, $receiverId, $type, $content, $mediaPath, $fileName, $uuid]);
$msgId = (int)$conn->lastInsertId();

// Create notification for receiver
$stN = $conn->prepare('INSERT INTO notifications (user_id, type, from_user_id, data) VALUES (?, "message", ?, ?)');
$stN->execute([$receiverId, $uid, json_encode(['message_id' => $msgId, 'peer_id' => $uid])]);

$stFetch = $conn->prepare('SELECT * FROM messages WHERE id=?');
$stFetch->execute([$msgId]);
out($stFetch->fetch());
?>
