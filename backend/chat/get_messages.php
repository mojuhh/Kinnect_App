<?php
require_once __DIR__ . '/../common.php';

$chatId = $_GET['chat_id'] ?? '';

if (empty($chatId)) {
    sendResponse('error', 'chat_id is required');
}

$conn = getDbConnection();
$stmt = $conn->prepare("SELECT * FROM messages WHERE chat_id = ? ORDER BY timestamp ASC");
$stmt->bind_param("s", $chatId);
$stmt->execute();
$result = $stmt->get_result();

$messages = [];
while ($row = $result->fetch_assoc()) {
    $messages[] = $row;
}

sendResponse('success', 'Messages fetched', ['messages' => $messages]);
?>
