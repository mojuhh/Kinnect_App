<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$beforeId = (int)($_GET['before_id'] ?? 0);
$limit = (int)($_GET['limit'] ?? 20);

$conn = db();

$where = 'WHERE (p.user_id = ? OR p.user_id IN (
    SELECT CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END
    FROM friendships WHERE (sender_id = ? OR receiver_id = ?) AND status = "accepted"
))';

if ($beforeId > 0) {
    $where .= " AND p.id < $beforeId";
}

$sql = "
    SELECT p.id, p.user_id, p.text, p.client_uuid, p.created_at,
           u.name, u.username, u.profile_pic,
           (SELECT type FROM reactions WHERE post_id = p.id AND user_id = ?) as my_reaction,
           (SELECT COUNT(*) FROM reactions WHERE post_id = p.id) as reaction_count,
           (SELECT COUNT(*) FROM comments WHERE post_id = p.id) as comment_count
    FROM posts p
    JOIN users u ON p.user_id = u.id
    $where
    ORDER BY p.id DESC
    LIMIT $limit
";

$st = $conn->prepare($sql);
$st->execute([$uid, $uid, $uid, $uid, $uid]);
$posts = $st->fetchAll();

foreach ($posts as &$p) {
    $stImg = $conn->prepare('SELECT path FROM post_images WHERE post_id = ? ORDER BY sort_order ASC');
    $stImg->execute([$p['id']]);
    $p['images'] = $stImg->fetchAll(PDO::FETCH_COLUMN);
}

out($posts);
?>
