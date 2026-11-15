<?php
require_once __DIR__ . '/../common.php';

$userId = $_POST['user_id'] ?? '';
$friendId = $_POST['friend_id'] ?? '';

if (empty($userId) || empty($friendId)) {
    sendResponse('error', 'user_id and friend_id are required');
}

$conn = getDbConnection();
$stmt = $conn->prepare("INSERT INTO friends (user_id, friend_id, status) VALUES (?, ?, 'accepted')");
$stmt->bind_param("ss", $userId, $friendId);

if ($stmt->execute()) {
    sendResponse('success', 'Friend request added');
} else {
    sendResponse('error', 'Failed to add friend');
}
?>
