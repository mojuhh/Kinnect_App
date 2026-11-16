<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$postId = (int)($in['post_id'] ?? 0);

if (!$postId) {
    fail('post_id required');
}

$conn = db();
$st = $conn->prepare('DELETE FROM posts WHERE id=? AND user_id=?');
$st->execute([$postId, $uid]);

if ($st->rowCount() > 0) {
    $stImg = $conn->prepare('DELETE FROM post_images WHERE post_id=?');
    $stImg->execute([$postId]);
    out(['message' => 'Post deleted']);
} else {
    fail('Post not found or unauthorized', 403);
}
?>
