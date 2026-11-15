<?php
require_once __DIR__ . '/../common.php';

$storyId = $_POST['story_id'] ?? uniqid('story_');
$userId = $_POST['user_id'] ?? '';
$username = $_POST['username'] ?? '';
$userAvatar = $_POST['user_avatar'] ?? '';
$mediaUri = $_POST['media_uri'] ?? '';
$mediaType = $_POST['media_type'] ?? 'image';
$textLabel = $_POST['text_label'] ?? '';
$audience = $_POST['audience'] ?? 'your_story';
$timestamp = $_POST['timestamp'] ?? (time() * 1000);
$expiresAt = $_POST['expires_at'] ?? ($timestamp + (24 * 60 * 60 * 1000));

if (empty($userId)) {
    sendResponse('error', 'user_id is required');
}

$conn = getDbConnection();
$stmt = $conn->prepare("INSERT INTO stories (story_id, user_id, username, user_avatar, media_uri, media_type, text_label, audience, timestamp, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
$stmt->bind_param("ssssssssii", $storyId, $userId, $username, $userAvatar, $mediaUri, $mediaType, $textLabel, $audience, $timestamp, $expiresAt);

if ($stmt->execute()) {
    sendResponse('success', 'Story created successfully', ['story_id' => $storyId]);
} else {
    sendResponse('error', 'Failed to create story');
}
?>
