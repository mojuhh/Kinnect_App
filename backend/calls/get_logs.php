<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$conn = db();

$sql = '
    SELECT c.*,
           CASE WHEN c.caller_id = ? THEN u2.name ELSE u1.name END as peer_name,
           CASE WHEN c.caller_id = ? THEN u2.profile_pic ELSE u1.profile_pic END as peer_profile_pic
    FROM calls c
    JOIN users u1 ON c.caller_id = u1.id
    JOIN users u2 ON c.receiver_id = u2.id
    WHERE c.caller_id = ? OR c.receiver_id = ?
    ORDER BY c.id DESC
    LIMIT 50
';

$st = $conn->prepare($sql);
$st->execute([$uid, $uid, $uid, $uid]);
out($st->fetchAll());
?>
