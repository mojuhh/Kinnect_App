<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$postId = (int)($in['post_id'] ?? 0);
$parentId = !empty($in['parent_id']) ? (int)$in['parent_id'] : null;
$text = trim($in['text'] ?? '');
$uuid = $in['client_uuid'] ?? fail('client_uuid required');

if (!$postId) {
    fail('post_id required');
}

$conn = db();

$st = $conn->prepare('SELECT c.*, u.name, u.username, u.profile_pic FROM comments c JOIN users u ON c.user_id = u.id WHERE c.client_uuid=?');
$st->execute([$uuid]);
if ($row = $st->fetch()) {
    out($row);
}

$imagePath = save_upload('image', 'comments', ['jpg', 'jpeg', 'png']);

$st = $conn->prepare('INSERT INTO comments (post_id, user_id, parent_id, text, image_path, client_uuid) VALUES (?, ?, ?, ?, ?, ?)');
$st->execute([$postId, $uid, $parentId, $text, $imagePath, $uuid]);
$commentId = (int)$conn->lastInsertId();

$stUser = $conn->prepare('SELECT name, username, profile_pic FROM users WHERE id=?');
$stUser->execute([$uid]);
$uData = $stUser->fetch();

out([
    'id' => $commentId,
    'post_id' => $postId,
    'user_id' => $uid,
    'parent_id' => $parentId,
    'text' => $text,
    'image_path' => $imagePath,
    'client_uuid' => $uuid,
    'created_at' => date('Y-m-d H:i:s'),
    'name' => $uData['name'],
    'username' => $uData['username'],
    'profile_pic' => $uData['profile_pic'],
    'like_count' => 0,
    'liked_by_me' => false
]);
?>
