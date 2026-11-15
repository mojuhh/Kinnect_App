<?php
require_once __DIR__ . '/../common.php';

$messageId = $_POST['message_id'] ?? uniqid('msg_');
$chatId = $_POST['chat_id'] ?? '';
$senderId = $_POST['sender_id'] ?? '';
$senderName = $_POST['sender_name'] ?? '';
$type = $_POST['type'] ?? 'text';
$content = $_POST['content'] ?? '';
$imageUrl = $_POST['image_url'] ?? '';
$replyToId = $_POST['reply_to_id'] ?? '';
$replyToText = $_POST['reply_to_text'] ?? '';
$timestamp = $_POST['timestamp'] ?? (time() * 1000);

if (empty($chatId) || empty($senderId)) {
    sendResponse('error', 'chat_id and sender_id are required');
}

$conn = getDbConnection();
$stmt = $conn->prepare("INSERT INTO messages (message_id, chat_id, sender_id, sender_name, type, content, image_url, reply_to_id, reply_to_text, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
$stmt->bind_param("sssssssssi", $messageId, $chatId, $senderId, $senderName, $type, $content, $imageUrl, $replyToId, $replyToText, $timestamp);

if ($stmt->execute()) {
    $stmtChat = $conn->prepare("INSERT INTO chats (chat_id, last_message, last_message_time, last_sender_id) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE last_message = VALUES(last_message), last_message_time = VALUES(last_message_time), last_sender_id = VALUES(last_sender_id)");
    $stmtChat->bind_param("ssis", $chatId, $content, $timestamp, $senderId);
    $stmtChat->execute();

    sendResponse('success', 'Message sent', ['message_id' => $messageId]);
} else {
    sendResponse('error', 'Failed to send message');
}
?>
