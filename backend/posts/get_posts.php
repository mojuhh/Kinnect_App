<?php
require_once __DIR__ . '/../common.php';

$conn = getDbConnection();
$result = $conn->query("SELECT * FROM posts ORDER BY timestamp DESC LIMIT 50");

$posts = [];
while ($row = $result->fetch_assoc()) {
    $posts[] = $row;
}

sendResponse('success', 'Posts fetched', ['posts' => $posts]);
?>
