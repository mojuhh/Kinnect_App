<?php
require_once __DIR__ . '/../common.php';

$postId = $_POST['post_id'] ?? uniqid('post_');
$userId = $_POST['user_id'] ?? '';
$username = $_POST['username'] ?? '';
$content = $_POST['content'] ?? '';
$imageUrl = $_POST['image_url'] ?? '';
$timestamp = $_POST['timestamp'] ?? (time() * 1000);

if (empty($userId)) {
    sendResponse('error', 'user_id is required');
}

$conn = getDbConnection();
$stmt = $conn->prepare("INSERT INTO posts (post_id, user_id, username, content, image_url, timestamp) VALUES (?, ?, ?, ?, ?, ?)");
$stmt->bind_param("sssssi", $postId, $userId, $username, $content, $imageUrl, $timestamp);

if ($stmt->execute()) {
    sendResponse('success', 'Post created successfully', ['post_id' => $postId]);
} else {
    sendResponse('error', 'Failed to create post');
}
?>
