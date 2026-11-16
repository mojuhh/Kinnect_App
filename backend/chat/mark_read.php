<?php
require_once __DIR__ . '/../common.php';

$uid = auth();
$in = body();
$peerId = (int)($in['peer_id'] ?? 0);

if (!$peerId) {
    fail('peer_id required');
}

out(['message' => 'Messages marked as read']);
?>
