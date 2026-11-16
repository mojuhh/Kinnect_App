<?php
require_once __DIR__ . '/../common.php';

$callerId = auth();
$in = body();
$receiverId = (int)($in['receiver_id'] ?? 0);
$type = $in['type'] ?? 'audio'; // audio | video
$status = $in['status'] ?? 'missed'; // missed | answered | declined
$startedAt = $in['started_at'] ?? date('Y-m-d H:i:s');
$duration = (int)($in['duration'] ?? 0);
$uuid = $in['client_uuid'] ?? fail('client_uuid required');

if (!$receiverId) {
    fail('receiver_id required');
}

$conn = db();
$st = $conn->prepare('SELECT * FROM calls WHERE client_uuid=?');
$st->execute([$uuid]);
if ($row = $st->fetch()) {
    out($row);
}

$stIns = $conn->prepare('INSERT INTO calls (caller_id, receiver_id, type, status, started_at, duration, client_uuid) VALUES (?, ?, ?, ?, ?, ?, ?)');
$stIns->execute([$callerId, $receiverId, $type, $status, $startedAt, $duration, $uuid]);

out(['message' => 'Call logged successfully']);
?>
