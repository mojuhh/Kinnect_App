<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$postId = (int)($in['post_id'] ?? 0);
$type = strtolower(trim($in['type'] ?? ''));

if (!$postId) {
    fail('post_id required');
}

$conn = db();

if (empty($type) || $type == 'none' || $type == 'null') {
    $st = $conn->prepare('DELETE FROM reactions WHERE post_id=? AND user_id=?');
    $st->execute([$postId, $uid]);
} else {
    $st = $conn->prepare('INSERT INTO reactions (post_id, user_id, type) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE type=VALUES(type), updated_at=CURRENT_TIMESTAMP');
    $st->execute([$postId, $uid, $type]);
}

$stCnt = $conn->prepare('SELECT COUNT(*) as count FROM reactions WHERE post_id=?');
$stCnt->execute([$postId]);
$count = (int)$stCnt->fetch()['count'];

out(['post_id' => $postId, 'my_reaction' => ($type == 'none' ? null : $type), 'reaction_count' => $count]);
?>
