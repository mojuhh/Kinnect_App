<?php
require_once __DIR__ . '/../common.php';

$postId = (int)($_GET['post_id'] ?? 0);

if (!$postId) {
    fail('post_id required');
}

$conn = db();
$st = $conn->prepare('
    SELECT r.type, u.id as user_id, u.name, u.username, u.profile_pic
    FROM reactions r
    JOIN users u ON r.user_id = u.id
    WHERE r.post_id = ?
');
$st->execute([$postId]);
$reactors = $st->fetchAll();

$counts = ['like' => 0, 'love' => 0, 'care' => 0, 'haha' => 0, 'wow' => 0, 'sad' => 0];
foreach ($reactors as $r) {
    if (isset($counts[$r['type']])) {
        $counts[$r['type']]++;
    }
}

out(['counts' => $counts, 'reactors' => $reactors]);
?>
