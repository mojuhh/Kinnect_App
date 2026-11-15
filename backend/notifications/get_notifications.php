<?php
require_once __DIR__ . '/../common.php';

$userId = $_GET['user_id'] ?? '';

if (empty($userId)) {
    sendResponse('error', 'user_id is required');
}

$conn = getDbConnection();
$stmt = $conn->prepare("SELECT * FROM notifications WHERE user_id = ? ORDER BY timestamp DESC LIMIT 50");
$stmt->bind_param("s", $userId);
$stmt->execute();
$result = $stmt->get_result();

$notifications = [];
while ($row = $result->fetch_assoc()) {
    $notifications[] = $row;
}

sendResponse('success', 'Notifications fetched', ['notifications' => $notifications]);
?>
