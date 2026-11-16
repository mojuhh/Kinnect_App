<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$postId = (int)($_GET['post_id'] ?? 0);

if (!$postId) {
    fail('post_id required');
}

$conn = db();
$sql = '
    SELECT c.*, u.name, u.username, u.profile_pic,
           (SELECT COUNT(*) FROM comment_likes WHERE comment_id = c.id) as like_count,
           EXISTS(SELECT 1 FROM comment_likes WHERE comment_id = c.id AND user_id = ?) as liked_by_me
    FROM comments c
    JOIN users u ON c.user_id = u.id
    WHERE c.post_id = ?
    ORDER BY c.id ASC
';

$st = $conn->prepare($sql);
$st->execute([$uid, $postId]);
$comments = $st->fetchAll();

foreach ($comments as &$c) {
    $c['liked_by_me'] = (bool)$c['liked_by_me'];
}

out($comments);
?>
