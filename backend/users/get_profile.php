<?php
require_once __DIR__ . '/../common.php';

$uid = $_GET['uid'] ?? '';

if (empty($uid)) {
    sendResponse('error', 'UID is required');
}

$conn = getDbConnection();
$stmt = $conn->prepare("SELECT * FROM users WHERE uid = ?");
$stmt->bind_param("s", $uid);
$stmt->execute();
$result = $stmt->get_result();

if ($user = $result->fetch_assoc()) {
    sendResponse('success', 'Profile fetched', ['profile' => $user]);
} else {
    sendResponse('error', 'Profile not found');
}
?>
