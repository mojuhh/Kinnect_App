<?php
require_once __DIR__ . '/../common.php';

$email = $_POST['email'] ?? '';
$password = $_POST['password'] ?? '';

if (empty($email) || empty($password)) {
    sendResponse('error', 'Email and password are required');
}

$conn = getDbConnection();
$stmt = $conn->prepare("SELECT uid, email, username, name FROM users WHERE email = ?");
$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();

if ($user = $result->fetch_assoc()) {
    sendResponse('success', 'Login successful', ['user' => $user]);
} else {
    sendResponse('error', 'User not found');
}
?>
