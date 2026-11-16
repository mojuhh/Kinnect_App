<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$conn = db();

$st = $conn->prepare('
    SELECT u.id, u.name, u.username, u.profile_pic, u.is_online, u.last_seen
    FROM users u
    INNER JOIN friendships f ON (f.sender_id = u.id OR f.receiver_id = u.id)
    WHERE (f.sender_id = ? OR f.receiver_id = ?)
      AND u.id != ?
      AND f.status = "accepted"
');
$st->execute([$uid, $uid, $uid]);
$friends = $st->fetchAll();

foreach ($friends as &$f) {
    $f['is_online'] = ($f['is_online'] == 1 && $f['last_seen'] && (strtotime($f['last_seen']) > time() - 45)) ? 1 : 0;
}

out($friends);
?>
