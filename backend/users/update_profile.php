<?php
require_once __DIR__ . '/../common.php';

$uid = $_POST['uid'] ?? '';
$name = $_POST['name'] ?? '';
$username = $_POST['username'] ?? '';
$bio = $_POST['bio'] ?? '';
$phone = $_POST['phone'] ?? '';

if (empty($uid)) {
    sendResponse('error', 'UID is required');
}

$conn = getDbConnection();
$stmt = $conn->prepare("UPDATE users SET name = ?, username = ?, bio = ?, phone = ? WHERE uid = ?");
$stmt->bind_param("sssss", $name, $username, $bio, $phone, $uid);

if ($stmt->execute()) {
    sendResponse('success', 'Profile updated successfully');
} else {
    sendResponse('error', 'Failed to update profile: ' . $stmt->error);
}
?>
