<?php
require_once __DIR__ . '/../common.php';

$callerId = $_POST['caller_id'] ?? '';
$receiverId = $_POST['receiver_id'] ?? '';
$callType = $_POST['call_type'] ?? 'voice'; // 'voice' or 'video'
$channelName = $_POST['channel_name'] ?? uniqid('channel_');

if (empty($callerId) || empty($receiverId)) {
    sendResponse('error', 'caller_id and receiver_id are required');
}

sendResponse('success', 'Call initiated', [
    'channel_name' => $channelName,
    'call_type' => $callType
]);
?>
