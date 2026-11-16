<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$uuid = $in['client_uuid'] ?? fail('client_uuid required');
$label = trim($in['label'] ?? '');
$audience = $in['audience'] ?? 'story'; // story | close_friends

$conn = db();

$st = $conn->prepare('SELECT s.*, u.name, u.username, u.profile_pic FROM stories s JOIN users u ON s.user_id = u.id WHERE s.client_uuid=?');
$st->execute([$uuid]);
if ($row = $st->fetch()) {
    out($row);
}

$mediaPath = save_upload('media', 'stories', ['jpg', 'jpeg', 'png', 'mp4']);
if (!$mediaPath) {
    fail('Media file required');
}

$ext = strtolower(pathinfo($mediaPath, PATHINFO_EXTENSION));
$mediaType = ($ext == 'mp4') ? 'video' : 'image';

$st = $conn->prepare('INSERT INTO stories (user_id, media_path, media_type, label, audience, client_uuid) VALUES (?, ?, ?, ?, ?, ?)');
$st->execute([$uid, $mediaPath, $mediaType, $label, $audience, $uuid]);
$storyId = (int)$conn->lastInsertId();

$stUser = $conn->prepare('SELECT name, username, profile_pic FROM users WHERE id=?');
$stUser->execute([$uid]);
$uData = $stUser->fetch();

out([
    'id' => $storyId,
    'user_id' => $uid,
    'media_path' => $mediaPath,
    'media_type' => $mediaType,
    'label' => $label,
    'audience' => $audience,
    'client_uuid' => $uuid,
    'created_at' => date('Y-m-d H:i:s'),
    'name' => $uData['name'],
    'username' => $uData['username'],
    'profile_pic' => $uData['profile_pic']
]);
?>
