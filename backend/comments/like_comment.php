<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$commentId = (int)($in['comment_id'] ?? 0);

if (!$commentId) {
    fail('comment_id required');
}

$conn = db();
$st = $conn->prepare('SELECT * FROM comment_likes WHERE comment_id=? AND user_id=?');
$st->execute([$commentId, $uid]);

if ($st->fetch()) {
    $stDel = $conn->prepare('DELETE FROM comment_likes WHERE comment_id=? AND user_id=?');
    $stDel->execute([$commentId, $uid]);
    $liked = false;
} else {
    $stIns = $conn->prepare('INSERT INTO comment_likes (comment_id, user_id) VALUES (?, ?)');
    $stIns->execute([$commentId, $uid]);
    $liked = true;
}

$stCnt = $conn->prepare('SELECT COUNT(*) as cnt FROM comment_likes WHERE comment_id=?');
$stCnt->execute([$commentId]);
$cnt = (int)$stCnt->fetch()['cnt'];

out(['comment_id' => $commentId, 'liked_by_me' => $liked, 'like_count' => $cnt]);
?>
