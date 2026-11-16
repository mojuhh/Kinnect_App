<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$conn = db();
$st = $conn->prepare('UPDATE users SET is_online=1, last_seen=UTC_TIMESTAMP() WHERE id=?');
$st->execute([$uid]);

out(['message' => 'Heartbeat updated']);
?>
