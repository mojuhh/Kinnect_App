<?php
require_once __DIR__ . '/../common.php';

$now = time() * 1000;
$conn = getDbConnection();
$stmt = $conn->prepare("SELECT * FROM stories WHERE expires_at > ? ORDER BY timestamp DESC");
$stmt->bind_param("i", $now);
$stmt->execute();
$result = $stmt->get_result();

$stories = [];
while ($row = $result->fetch_assoc()) {
    $stories[] = $row;
}

sendResponse('success', 'Active stories fetched', ['stories' => $stories]);
?>
