<?php
require_once __DIR__ . '/../common.php';

$callerId = auth();
$in = body();
$receiverId = (int)($in['receiver_id'] ?? 0);
$callType = $in['call_type'] ?? 'audio'; // audio | video
$channelName = $in['channel_name'] ?? ('call_' . min($callerId, $receiverId) . '_' . max($callerId, $receiverId));

if (!$receiverId) {
    fail('receiver_id required');
}

$conn = db();
$stN = $conn->prepare('INSERT INTO notifications (user_id, type, from_user_id, data) VALUES (?, "incoming_call", ?, ?)');
$stN->execute([$receiverId, $callerId, json_encode(['channel_name' => $channelName, 'call_type' => $callType])]);

out([
    'channel_name' => $channelName,
    'call_type' => $callType,
    'status' => 'calling'
]);
?>
