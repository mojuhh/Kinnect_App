<?php
require_once __DIR__ . '/../common.php';

$userId = $_GET['user_id'] ?? '';

if (empty($userId)) {
    sendResponse('error', 'user_id is required');
}

$conn = getDbConnection();
$likePattern = "%" . $userId . "%";
$stmt = $conn->prepare("SELECT * FROM chats WHERE chat_id LIKE ? ORDER BY last_message_time DESC");
$stmt->bind_param("s", $likePattern);
$stmt->execute();
$result = $stmt->get_result();

$chats = [];
while ($row = $result->fetch_assoc()) {
    $chats[] = $row;
}

sendResponse('success', 'Chats fetched', ['chats' => $chats]);
?>
