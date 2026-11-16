<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$action = $in['action'] ?? 'list'; // list | add | remove
$friendId = (int)($in['friend_id'] ?? 0);

$conn = db();

if ($action == 'add' && $friendId) {
    $st = $conn->prepare('INSERT IGNORE INTO close_friends (user_id, friend_id) VALUES (?, ?)');
    $st->execute([$uid, $friendId]);
    out(['message' => 'Added to close friends']);
} else if ($action == 'remove' && $friendId) {
    $st = $conn->prepare('DELETE FROM close_friends WHERE user_id=? AND friend_id=?');
    $st->execute([$uid, $friendId]);
    out(['message' => 'Removed from close friends']);
} else {
    $st = $conn->prepare('
        SELECT u.id, u.name, u.username, u.profile_pic
        FROM users u
        JOIN close_friends cf ON cf.friend_id = u.id
        WHERE cf.user_id = ?
    ');
    $st->execute([$uid]);
    out($st->fetchAll());
}
?>
