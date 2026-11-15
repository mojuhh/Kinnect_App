<?php
require_once __DIR__ . '/../common.php';

$uid = $_POST['uid'] ?? '';
$email = $_POST['email'] ?? '';

if (empty($uid) || empty($email)) {
    sendResponse('error', 'UID and email are required');
}

$conn = getDbConnection();
$stmt = $conn->prepare("INSERT INTO users (uid, email, status) VALUES (?, ?, 'online') ON DUPLICATE KEY UPDATE email = VALUES(email)");
$stmt->bind_param("ss", $uid, $email);

if ($stmt->execute()) {
    sendResponse('success', 'User registered successfully');
} else {
    sendResponse('error', 'Registration failed: ' . $stmt->error);
}
?>
