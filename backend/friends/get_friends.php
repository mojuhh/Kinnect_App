<?php
require_once __DIR__ . '/../common.php';

$uid = $_GET['uid'] ?? '';

if (empty($uid)) {
    sendResponse('error', 'UID is required');
}

$conn = getDbConnection();
$stmt = $conn->prepare("SELECT u.* FROM users u INNER JOIN friends f ON (f.friend_id = u.uid OR f.user_id = u.uid) WHERE (f.user_id = ? OR f.friend_id = ?) AND u.uid != ? AND f.status = 'accepted'");
$stmt->bind_param("sss", $uid, $uid, $uid);
$stmt->execute();
$result = $stmt->get_result();

$friends = [];
while ($row = $result->fetch_assoc()) {
    $friends[] = $row;
}

sendResponse('success', 'Friends list fetched', ['friends' => $friends]);
?>
