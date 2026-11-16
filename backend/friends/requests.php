<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$conn = db();

$st = $conn->prepare('
    SELECT u.id, u.name, u.username, u.profile_pic, f.created_at as request_time
    FROM users u
    INNER JOIN friendships f ON f.sender_id = u.id
    WHERE f.receiver_id = ? AND f.status = "pending"
');
$st->execute([$uid]);
$requests = $st->fetchAll();

out($requests);
?>
