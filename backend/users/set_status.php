<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$isOnline = isset($in['online']) && ($in['online'] === true || $in['online'] === 1 || $in['online'] === '1') ? 1 : 0;

$conn = db();
$st = $conn->prepare('UPDATE users SET is_online=?, last_seen=UTC_TIMESTAMP() WHERE id=?');
$st->execute([$isOnline, $uid]);

out(['message' => 'Status updated']);
?>
